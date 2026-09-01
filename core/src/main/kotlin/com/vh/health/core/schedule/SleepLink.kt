package com.vh.health.core.schedule

import java.time.LocalTime

/**
 * Keeps the two anchors honest with each other.
 *
 * By default the bedtime is derived from the wake time and a sleep target, so
 * pushing the morning anchor to 05:00 proposes a 21:00 bedtime. The user may unlink
 * them, in which case the app only reports what the gap actually is rather than
 * moving anything.
 */
object SleepLink {

    fun bedtimeFor(wake: LocalTime, sleepTargetMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES): LocalTime {
        require(sleepTargetMinutes in 1 until MINUTES_PER_DAY) {
            "a sleep target of $sleepTargetMinutes minutes makes no sense"
        }
        return wake.minusMinutes(sleepTargetMinutes.toLong())
    }

    fun wakeFor(bedtime: LocalTime, sleepTargetMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES): LocalTime {
        require(sleepTargetMinutes in 1 until MINUTES_PER_DAY) {
            "a sleep target of $sleepTargetMinutes minutes makes no sense"
        }
        return bedtime.plusMinutes(sleepTargetMinutes.toLong())
    }

    /** How long the user actually sleeps with these two anchors, wrapping midnight. */
    fun sleepMinutes(bedtime: LocalTime, wake: LocalTime): Int = minutesBetween(bedtime, wake)

    fun deviationFromTarget(
        bedtime: LocalTime,
        wake: LocalTime,
        sleepTargetMinutes: Int = DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES,
    ): Int = sleepMinutes(bedtime, wake) - sleepTargetMinutes
}
