package com.vh.health.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vh.health.AppContainer
import com.vh.health.core.content.Weekday
import com.vh.health.core.content.Workout
import com.vh.health.core.program.Phase
import com.vh.health.core.program.Progression
import com.vh.health.core.schedule.Anchor
import com.vh.health.core.schedule.BlockState
import com.vh.health.core.schedule.DayPhase
import com.vh.health.core.schedule.DayTemplates
import com.vh.health.core.schedule.SleepLink
import com.vh.health.core.schedule.Timeline
import com.vh.health.core.schedule.TimelineEngine
import com.vh.health.core.schedule.TimelinePosition
import com.vh.health.core.schedule.blockStates
import com.vh.health.core.schedule.positionAt
import com.vh.health.data.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TodayUiState(
    val settings: AppSettings = AppSettings(),
    val now: LocalTime = LocalTime.MIDNIGHT,
    val morning: Timeline = Timeline(emptyList()),
    val morningPosition: TimelinePosition = TimelinePosition(DayPhase.BEFORE, 0f, null, 0, 0),
    val morningStates: Map<String, BlockState> = emptyMap(),
    val evening: Timeline = Timeline(emptyList()),
    val eveningStates: Map<String, BlockState> = emptyMap(),
    val weekday: Weekday = Weekday.MONDAY,
    val workout: Workout? = null,
    val sleepMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES,
    val week: Int = 1,
    val phase: Phase = Phase.ADAPT,
)

class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    private val now = MutableStateFlow(LocalTime.now())

    init {
        viewModelScope.launch { container.settings.ensureProgramStart() }

        viewModelScope.launch {
            while (true) {
                now.value = LocalTime.now()
                delay(TICK_MILLIS)
            }
        }

        viewModelScope.launch {
            combine(container.settings.settings, now) { settings, clock -> project(settings, clock) }
                .collect { _state.value = it }
        }
    }

    /** Nudges today's start without touching the saved wake time. */
    fun nudgeStart(minutes: Int) {
        val current = _state.value.settings.effectiveStart
        viewModelScope.launch { container.settings.setTodayStart(current.plusMinutes(minutes.toLong())) }
    }

    fun resetToday() {
        viewModelScope.launch {
            container.settings.setTodayStart(null)
            container.settings.setTodayWindow(null)
        }
    }

    /** "Sáng nay chỉ có N phút." Pass null to restore the full morning. */
    fun setWindow(minutes: Int?) {
        viewModelScope.launch { container.settings.setTodayWindow(minutes) }
    }

    private fun project(settings: AppSettings, clock: LocalTime): TodayUiState {
        val start = settings.effectiveStart

        val morning = TimelineEngine.build(
            blocks = DayTemplates.morning(settings.mainSessionMinutes),
            anchor = settings.todayWindowMinutes
                ?.let { Anchor.Window(start, start.plusMinutes(it.toLong())) }
                ?: Anchor.StartAt(start),
        )
        val evening = TimelineEngine.build(DayTemplates.evening(), Anchor.FinishBy(settings.bedtime))
        val weekday = container.content.weekdayToday()
        val week = settings.programStartEpochDay?.let { Progression.weekNumber(it) } ?: 1

        return TodayUiState(
            settings = settings,
            now = clock,
            morning = morning,
            morningPosition = morning.positionAt(clock),
            morningStates = morning.blockStates(clock),
            evening = evening,
            eveningStates = evening.blockStates(clock),
            weekday = weekday,
            workout = container.content.program.workoutFor(weekday),
            sleepMinutes = SleepLink.sleepMinutes(settings.bedtime, settings.wakeTime),
            week = week,
            phase = Progression.phaseOf(week),
        )
    }

    companion object {
        private const val TICK_MILLIS = 30_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TodayViewModel(container) as T
            }
    }
}
