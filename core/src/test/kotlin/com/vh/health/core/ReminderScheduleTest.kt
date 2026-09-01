package com.vh.health.core

import com.vh.health.core.schedule.ReminderKind
import com.vh.health.core.schedule.ReminderSchedule
import com.vh.health.core.schedule.nextOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderScheduleTest {

    private val day = LocalDate.of(2026, 9, 1)
    private fun at(h: Int, m: Int) = LocalTime.of(h, m)
    private fun dt(h: Int, m: Int) = LocalDateTime.of(day, at(h, m))

    @Test
    fun `a target later today stays today`() {
        assertEquals(day.atTime(at(9, 30)), nextOccurrence(at(9, 30), dt(4, 30)))
    }

    @Test
    fun `a target already passed today rolls to tomorrow`() {
        assertEquals(day.plusDays(1).atTime(at(4, 30)), nextOccurrence(at(4, 30), dt(9, 30)))
    }

    @Test
    fun `a target exactly equal to now rolls to tomorrow rather than firing again immediately`() {
        assertEquals(day.plusDays(1).atTime(at(4, 30)), nextOccurrence(at(4, 30), dt(4, 30)))
    }

    @Test
    fun `a target just after midnight from just before it only advances by minutes, not a full day`() {
        val now = LocalDateTime.of(day, at(23, 50))
        val next = nextOccurrence(at(0, 10), now)
        assertEquals(day.plusDays(1).atTime(at(0, 10)), next)
        assertEquals(20, java.time.Duration.between(now, next).toMinutes())
    }

    @Test
    fun `default anchors produce exactly the five moments named in the approved plan`() {
        val times = ReminderSchedule.times(wakeTime = at(4, 30), bedtime = at(20, 30))
        assertEquals(
            mapOf(
                ReminderKind.MORNING_START to at(4, 30),
                ReminderKind.DESK_BREAK_1 to at(9, 30),
                ReminderKind.DESK_BREAK_2 to at(14, 0),
                ReminderKind.DESK_BREAK_3 to at(16, 30),
                ReminderKind.EVENING_WIND_DOWN to at(19, 45),
            ),
            times.associate { it.kind to it.time },
        )
    }

    @Test
    fun `moving the wake anchor moves only the morning reminder`() {
        val default = ReminderSchedule.times(at(4, 30), at(20, 30))
        val shifted = ReminderSchedule.times(at(5, 0), at(20, 30))

        assertEquals(at(5, 0), shifted.first { it.kind == ReminderKind.MORNING_START }.time)
        // Everything else is anchored to work hours or bedtime, not wake time.
        listOf(ReminderKind.DESK_BREAK_1, ReminderKind.DESK_BREAK_2, ReminderKind.DESK_BREAK_3, ReminderKind.EVENING_WIND_DOWN)
            .forEach { kind ->
                assertEquals(
                    default.first { it.kind == kind }.time,
                    shifted.first { it.kind == kind }.time,
                    "$kind should not move when only the wake anchor changes",
                )
            }
    }

    @Test
    fun `moving bedtime moves the evening reminder by the same amount`() {
        val times = ReminderSchedule.times(at(4, 30), at(21, 0))
        assertEquals(at(20, 15), times.first { it.kind == ReminderKind.EVENING_WIND_DOWN }.time)
    }

    @Test
    fun `every reminder kind has a distinct, stable request code`() {
        val codes = ReminderKind.entries.map { it.requestCode }
        assertEquals(codes.distinct().size, codes.size, "request codes must be unique")
        assertTrue(codes.all { it > 0 })
    }
}
