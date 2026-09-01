package com.vh.health.core

import com.vh.health.core.schedule.Anchor
import com.vh.health.core.schedule.BlockState
import com.vh.health.core.schedule.DayPhase
import com.vh.health.core.schedule.DayTemplates
import com.vh.health.core.schedule.TimelineEngine
import com.vh.health.core.schedule.blockStates
import com.vh.health.core.schedule.positionAt
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineClockTest {

    private val morning = TimelineEngine.build(
        DayTemplates.morning(),
        Anchor.StartAt(DayTemplates.DEFAULT_WAKE),
    )

    private fun at(h: Int, m: Int) = LocalTime.of(h, m)

    @Test
    fun `at the very start the day has begun and nothing has elapsed`() {
        val position = morning.positionAt(at(4, 30))
        assertEquals(DayPhase.DURING, position.phase)
        assertEquals(0f, position.progress)
        assertEquals("personal_care", position.currentBlockId)
        assertEquals(105, position.minutesRemaining)
    }

    @Test
    fun `mid warm-up the current block is the warm-up`() {
        val position = morning.positionAt(at(4, 45))
        assertEquals(DayPhase.DURING, position.phase)
        assertEquals("warm_up", position.currentBlockId)
    }

    @Test
    fun `half way through the main session progress reads about a third`() {
        // 04:52 + 16′ = 05:08, which is 38 of 105 minutes in.
        val position = morning.positionAt(at(5, 8))
        assertEquals("main", position.currentBlockId)
        assertEquals(38f / 105f, position.progress, 1e-4f)
    }

    @Test
    fun `the morning shown at 0838 has plainly already finished`() {
        val position = morning.positionAt(at(8, 38))
        assertEquals(DayPhase.AFTER, position.phase)
        assertEquals(1f, position.progress)
    }

    @Test
    fun `at 0300 the morning has not started yet`() {
        val position = morning.positionAt(at(3, 0))
        assertEquals(DayPhase.BEFORE, position.phase)
        assertEquals(90, position.minutesUntilStart)
    }

    @Test
    fun `in the evening the next morning counts as upcoming, not long past`() {
        val position = morning.positionAt(at(20, 0))
        assertEquals(DayPhase.BEFORE, position.phase)
        assertEquals(8 * 60 + 30, position.minutesUntilStart)
    }

    @Test
    fun `the last minute of the day is still inside it`() {
        assertEquals(DayPhase.DURING, morning.positionAt(at(6, 14)).phase)
        assertEquals(DayPhase.AFTER, morning.positionAt(at(6, 15)).phase)
    }

    @Test
    fun `block states split cleanly around the current block`() {
        val states = morning.blockStates(at(4, 45))
        assertEquals(BlockState.PAST, states["personal_care"])
        assertEquals(BlockState.CURRENT, states["warm_up"])
        assertEquals(BlockState.UPCOMING, states["main"])
        assertEquals(BlockState.UPCOMING, states["breakfast"])
    }

    @Test
    fun `once the day is over every block reads as past`() {
        val states = morning.blockStates(at(8, 38))
        assertTrue(states.values.all { it == BlockState.PAST })
        assertEquals(morning.blocks.size, states.size)
    }

    @Test
    fun `before the day starts every block reads as upcoming`() {
        val states = morning.blockStates(at(3, 0))
        assertTrue(states.values.all { it == BlockState.UPCOMING })
    }

    @Test
    fun `an empty timeline does not blow up`() {
        val empty = TimelineEngine.build(emptyList(), Anchor.StartAt(at(5, 0)))
        assertEquals(DayPhase.BEFORE, empty.positionAt(at(5, 0)).phase)
        assertTrue(empty.blockStates(at(5, 0)).isEmpty())
    }
}
