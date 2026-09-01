package com.vh.health.core.schedule

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The five moments the app reminds the user about via a notification — never an
 * alarm-clock UI, per the user's explicit choice. [requestCode] is stable and
 * explicit (not `ordinal`) because the Android side uses it as an AlarmManager /
 * PendingIntent request code, where silently renumbering on a reorder would leak or
 * collide with a previously-scheduled alarm.
 */
enum class ReminderKind(val requestCode: Int) {
    MORNING_START(1),
    DESK_BREAK_1(2),
    DESK_BREAK_2(3),
    DESK_BREAK_3(4),
    EVENING_WIND_DOWN(5),
}

data class ReminderTime(val kind: ReminderKind, val time: LocalTime)

/**
 * The next real instant [target] occurs at or after [now] — today if it hasn't
 * happened yet, tomorrow otherwise. AlarmManager needs a concrete date-time; this is
 * the one place a bare LocalTime becomes one, so "did today-vs-tomorrow come out
 * right" is a unit test instead of a reminder that silently fires a day early or a
 * day late.
 */
fun nextOccurrence(target: LocalTime, now: LocalDateTime): LocalDateTime {
    val todayAt = now.toLocalDate().atTime(target)
    return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
}

object ReminderSchedule {

    /**
     * Where each reminder falls today, given the two anchors that actually govern
     * them. Desk breaks hang off the working day (see [DayTemplates]), not off
     * either anchor, so moving the wake time never moves them — matches
     * [DayTemplates.DEFAULT_DESK_BREAKS]'s own doc comment.
     */
    fun times(wakeTime: LocalTime, bedtime: LocalTime): List<ReminderTime> {
        val evening = TimelineEngine.build(DayTemplates.evening(), Anchor.FinishBy(bedtime))
        val windDownStart = evening.block("wind_down")?.start ?: bedtime.minusMinutes(45)

        val deskBreaks = DayTemplates.DEFAULT_DESK_BREAKS.mapIndexed { index, time ->
            val kind = when (index) {
                0 -> ReminderKind.DESK_BREAK_1
                1 -> ReminderKind.DESK_BREAK_2
                else -> ReminderKind.DESK_BREAK_3
            }
            ReminderTime(kind, time)
        }

        return listOf(
            ReminderTime(ReminderKind.MORNING_START, wakeTime),
            *deskBreaks.toTypedArray(),
            ReminderTime(ReminderKind.EVENING_WIND_DOWN, windDownStart),
        )
    }
}
