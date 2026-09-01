package com.vh.health.ui.player

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vh.health.AppContainer
import com.vh.health.audio.BeatEngine
import com.vh.health.audio.PlaybackFocus
import com.vh.health.audio.VoiceCues
import com.vh.health.core.content.Workout
import com.vh.health.core.program.KneeLoadPolicy
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.program.applyCardioLoadFactor
import com.vh.health.core.session.SessionBuilder
import com.vh.health.core.session.SessionStep
import com.vh.health.core.session.StepPhase
import com.vh.health.core.session.cursorAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlayerUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val steps: List<SessionStep> = emptyList(),
    val stepIndex: Int = 0,
    val remainingInStep: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
) {
    val currentStep: SessionStep? get() = steps.getOrNull(stepIndex)
    val nextStep: SessionStep? get() = steps.getOrNull(stepIndex + 1)
    val isCountIn: Boolean get() = !isFinished && remainingInStep in 1..3
    val totalSeconds: Int get() = steps.sumOf { it.seconds }
    val elapsedSeconds: Int
        get() = steps.take(stepIndex).sumOf { it.seconds } + (currentStep?.let { it.seconds - remainingInStep } ?: 0)
}

/**
 * Owns the running clock for one workout session. The timing math itself —
 * "where does second N land" — is [com.vh.health.core.session.SessionBuilder] and
 * `cursorAt`, both pure and unit-tested; this class just ticks real time into that
 * function and turns the crossings it reports into sound.
 *
 * The workout it plays is not necessarily the one authored in `program.json` verbatim:
 * [applyCardioLoadFactor] scales its cardio content by whatever the most recent knee
 * check-in decided (see [KneeLoadPolicy]), so a week the knees flagged OVERLOADED
 * actually plays a shorter cardio block next time, not just a number nobody acts on.
 */
class WorkoutPlayerViewModel(
    private val container: AppContainer,
    private val workoutId: String,
) : ViewModel() {

    private val beat = BeatEngine()
    private val voice = VoiceCues(container.appContext)
    private val focus = PlaybackFocus(container.appContext)

    private val todayEpochDay = LocalDate.now().toEpochDay()

    private var workout: Workout? = null
    private var steps: List<SessionStep> = emptyList()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var elapsedMs = 0L
    private var lastStepIndex = -1
    private var lastRemaining = Int.MIN_VALUE
    private var lastTickRealtime = 0L

    init {
        viewModelScope.launch {
            val (loadedWorkout, loadedSteps) = loadWorkout()
            workout = loadedWorkout
            steps = loadedSteps
            _state.value = PlayerUiState(
                isLoading = false,
                workout = loadedWorkout,
                steps = loadedSteps,
                remainingInStep = loadedSteps.firstOrNull()?.seconds ?: 0,
            )

            if (loadedSteps.isNotEmpty()) {
                focus.requestDucking()
                lastTickRealtime = SystemClock.elapsedRealtime()
                while (isActive) {
                    delay(TICK_MS)
                    val now = SystemClock.elapsedRealtime()
                    if (_state.value.isRunning) {
                        elapsedMs += now - lastTickRealtime
                        applyCursor()
                    }
                    lastTickRealtime = now
                }
            }
        }
    }

    /** Reads today's cardio-load factor from the most recent knee check-in (any
     *  workout, not just this one — see [applyCardioLoadFactor]) and applies it
     *  before building the session steps, so the very first PREPARE step already
     *  reflects it rather than jumping mid-playback. */
    private suspend fun loadWorkout(): Pair<Workout?, List<SessionStep>> {
        val base = container.content.program.workout(workoutId) ?: return null to emptyList()
        val mostRecentSignal = container.progress.kneeCheckIns.first()
            .maxByOrNull { it.epochDay }
            ?.signal
        val adjusted = applyCardioLoadFactor(base, KneeLoadPolicy.impactFactorFor(mostRecentSignal), container.content.library)
        return adjusted to SessionBuilder.build(adjusted)
    }

    fun start() {
        if (_state.value.isFinished || _state.value.isLoading) return
        lastTickRealtime = SystemClock.elapsedRealtime()
        _state.update { it.copy(isRunning = true) }
    }

    fun pause() {
        _state.update { it.copy(isRunning = false) }
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    fun skipToNext() = jumpToStep(_state.value.stepIndex + 1)

    fun skipToPrevious() = jumpToStep((_state.value.stepIndex - 1).coerceAtLeast(0))

    /** Logs today's knee signal for this workout and lets the screen move on —
     *  called from the finished-state picker, only ever shown when
     *  `workout.tracksKneeSignal` is true. */
    fun recordKneeSignal(signal: KneeSignal) {
        val id = workout?.id ?: return
        viewModelScope.launch { container.progress.logKneeCheckIn(todayEpochDay, id, signal) }
    }

    private fun jumpToStep(index: Int) {
        if (_state.value.isLoading) return
        elapsedMs = if (index in steps.indices) {
            steps.take(index).sumOf { it.seconds } * 1000L
        } else {
            steps.sumOf { it.seconds } * 1000L
        }
        applyCursor()
    }

    private fun applyCursor() {
        val elapsedSeconds = (elapsedMs / 1000).toInt()
        val cursor = steps.cursorAt(elapsedSeconds)

        if (cursor.stepIndex != lastStepIndex) {
            announceStep(steps.getOrNull(cursor.stepIndex))
            lastStepIndex = cursor.stepIndex
            lastRemaining = Int.MIN_VALUE
        }
        if (cursor.remainingInStep != lastRemaining) {
            if (cursor.isCountIn) beat.countInTick()
            lastRemaining = cursor.remainingInStep
        }
        if (cursor.finished && !_state.value.isFinished) {
            beat.finished()
            voice.announce("Hoàn thành buổi tập. Làm tốt lắm.")
            workout?.let { finished -> viewModelScope.launch { container.progress.logSession(todayEpochDay, finished.id) } }
        }

        _state.update {
            it.copy(
                stepIndex = cursor.stepIndex,
                remainingInStep = cursor.remainingInStep,
                isFinished = cursor.finished,
                isRunning = it.isRunning && !cursor.finished,
            )
        }
    }

    private fun announceStep(step: SessionStep?) {
        step ?: return
        val exerciseName = step.exerciseId?.let { container.content.library[it]?.nameVi }
        when (step.phase) {
            StepPhase.PREPARE -> voice.announce("Chuẩn bị. ${exerciseName.orEmpty()}")
            StepPhase.WORK -> {
                beat.workStart()
                voice.announce(exerciseName ?: "Bắt đầu")
            }
            StepPhase.REST -> {
                beat.restStart()
                voice.announce(if (exerciseName != null) "Nghỉ. Tiếp theo: $exerciseName" else "Nghỉ")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        beat.release()
        voice.release()
        focus.release()
    }

    companion object {
        private const val TICK_MS = 200L

        fun factory(container: AppContainer, workoutId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutPlayerViewModel(container, workoutId) as T
            }
    }
}
