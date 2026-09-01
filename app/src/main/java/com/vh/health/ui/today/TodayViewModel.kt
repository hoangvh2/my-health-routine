package com.vh.health.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vh.health.AppContainer
import com.vh.health.core.content.Weekday
import com.vh.health.core.content.Workout
import com.vh.health.core.schedule.Anchor
import com.vh.health.core.schedule.DayTemplates
import com.vh.health.core.schedule.SleepLink
import com.vh.health.core.schedule.Timeline
import com.vh.health.core.schedule.TimelineEngine
import com.vh.health.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

data class TodayUiState(
    val settings: AppSettings = AppSettings(),
    val morning: Timeline = Timeline(emptyList()),
    val evening: Timeline = Timeline(emptyList()),
    val weekday: Weekday = Weekday.MONDAY,
    val workout: Workout? = null,
    val sleepMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES,
)

class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.settings.collect { settings -> _state.value = project(settings) }
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

    private fun project(settings: AppSettings): TodayUiState {
        val start: LocalTime = settings.effectiveStart
        val blocks = DayTemplates.morning(settings.mainSessionMinutes)

        val morning = TimelineEngine.build(
            blocks = blocks,
            anchor = settings.todayWindowMinutes
                ?.let { Anchor.Window(start, start.plusMinutes(it.toLong())) }
                ?: Anchor.StartAt(start),
        )

        val evening = TimelineEngine.build(DayTemplates.evening(), Anchor.FinishBy(settings.bedtime))
        val weekday = container.content.weekdayToday()

        return TodayUiState(
            settings = settings,
            morning = morning,
            evening = evening,
            weekday = weekday,
            workout = container.content.program.workoutFor(weekday),
            sleepMinutes = SleepLink.sleepMinutes(settings.bedtime, settings.wakeTime),
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TodayViewModel(container) as T
            }
    }
}
