package com.vh.health.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vh.health.AppContainer
import com.vh.health.core.program.KneeLoadPolicy
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.program.currentStreak
import com.vh.health.core.progress.BodyMetric
import com.vh.health.core.progress.KneeCheckIn
import com.vh.health.core.progress.SessionLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SessionSummary(val date: LocalDate, val workoutTitleVi: String)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val streak: Int = 0,
    val recentSessions: List<SessionSummary> = emptyList(),
    val latestKneeSignal: KneeSignal? = null,
    val suggestClinician: Boolean = false,
    val bodyMetrics: List<BodyMetric> = emptyList(),
)

/**
 * Joins the three `ProgressRepository` streams with the bundled programme (for
 * workout titles) into one screen state. `currentStreak` and `shouldSuggestClinician`
 * are the same pure `:core` functions their own unit tests exercise directly — this
 * class only supplies them real data and keeps the result live.
 */
class ProgressViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.progress.sessions,
                container.progress.kneeCheckIns,
                container.progress.bodyMetrics,
            ) { sessions, kneeCheckIns, metrics -> project(sessions, kneeCheckIns, metrics) }
                .collect { _state.value = it }
        }
    }

    /** Either argument may be null — the user may only be tracking one metric. A
     *  call with both null is a no-op rather than an empty logged point. */
    fun saveBodyMetric(weightKg: Double?, waistCm: Double?) {
        if (weightKg == null && waistCm == null) return
        viewModelScope.launch {
            container.progress.logBodyMetric(LocalDate.now().toEpochDay(), weightKg, waistCm)
        }
    }

    private fun project(sessions: List<SessionLog>, kneeCheckIns: List<KneeCheckIn>, metrics: List<BodyMetric>): ProgressUiState {
        val sessionDates = sessions.map { LocalDate.ofEpochDay(it.epochDay) }.toSet()
        val recent = sessions
            .sortedByDescending { it.epochDay }
            .take(RECENT_SESSIONS_SHOWN)
            .map { log ->
                SessionSummary(
                    date = LocalDate.ofEpochDay(log.epochDay),
                    workoutTitleVi = container.content.program.workout(log.workoutId)?.titleVi ?: log.workoutId,
                )
            }
        // Chronological, oldest first: shouldSuggestClinician reads the tail of the
        // list as "most recent", the same convention its own unit tests pin.
        val orderedSignals = kneeCheckIns.sortedBy { it.epochDay }.map { it.signal }

        return ProgressUiState(
            isLoading = false,
            streak = currentStreak(sessionDates),
            recentSessions = recent,
            latestKneeSignal = orderedSignals.lastOrNull(),
            suggestClinician = KneeLoadPolicy.shouldSuggestClinician(orderedSignals),
            bodyMetrics = metrics.sortedByDescending { it.epochDay },
        )
    }

    companion object {
        private const val RECENT_SESSIONS_SHOWN = 10

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProgressViewModel(container) as T
            }
    }
}
