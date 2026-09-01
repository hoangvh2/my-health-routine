package com.vh.health.data

import com.vh.health.core.schedule.DayTemplates
import java.time.LocalTime

/**
 * Everything the timeline needs in order to be computed. No block times live here —
 * only the anchors and the durations they drive.
 */
data class AppSettings(
    val wakeTime: LocalTime = DayTemplates.DEFAULT_WAKE,
    val bedtime: LocalTime = DayTemplates.DEFAULT_BEDTIME,
    val sleepTargetMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES,
    val bedtimeFollowsWake: Boolean = true,
    val mainSessionMinutes: Int = 33,
    /** A one-off start time for today only; cleared automatically tomorrow. */
    val todayStartOverride: LocalTime? = null,
    /** "Sáng nay chỉ có N phút" — null means the morning is not being squeezed. */
    val todayWindowMinutes: Int? = null,
) {
    /** The start the morning chain is actually anchored to right now. */
    val effectiveStart: LocalTime get() = todayStartOverride ?: wakeTime
}
