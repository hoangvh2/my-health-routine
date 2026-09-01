package com.vh.health.core

import com.vh.health.core.program.currentStreak
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {

    private val today = LocalDate.of(2026, 9, 3) // a Thursday

    @Test
    fun `no sessions ever is a streak of zero`() {
        assertEquals(0, currentStreak(emptySet(), today))
    }

    @Test
    fun `three consecutive days ending today counts three`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, currentStreak(dates, today))
    }

    @Test
    fun `a gap two days back stops the count there`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(3)) // day -2 missing
        assertEquals(2, currentStreak(dates, today))
    }

    @Test
    fun `today not logged yet does not zero a real streak — the day isn't over`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(3, currentStreak(dates, today))
    }

    @Test
    fun `today not logged and yesterday also missing is a genuine broken streak`() {
        val dates = setOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, currentStreak(dates, today))
    }

    @Test
    fun `a single session today with nothing before it counts one`() {
        assertEquals(1, currentStreak(setOf(today), today))
    }
}
