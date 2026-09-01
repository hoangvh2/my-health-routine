package com.vh.health.core

import com.vh.health.core.schedule.Anchor
import com.vh.health.core.schedule.BlockKind
import com.vh.health.core.schedule.DayBlock
import com.vh.health.core.schedule.DayTemplates
import com.vh.health.core.schedule.Priority
import com.vh.health.core.schedule.TimelineEngine
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimelineEngineTest {

    private fun at(h: Int, m: Int) = LocalTime.of(h, m)

    @Test
    fun `morning anchored at 0430 matches the approved plan`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(), Anchor.StartAt(DayTemplates.DEFAULT_WAKE))

        assertEquals(
            listOf(
                "personal_care" to at(4, 30),
                "warm_up" to at(4, 40),
                "main" to at(4, 52),
                "cool_down" to at(5, 25),
                "shower" to at(5, 35),
                "breakfast" to at(5, 55),
            ),
            timeline.blocks.map { it.block.id to it.start },
        )
        assertEquals(at(6, 15), timeline.end)
        assertEquals(105, timeline.totalMinutes)
        assertFalse(timeline.wasCompressed)
    }

    @Test
    fun `moving the anchor to 0500 shifts every block by exactly thirty minutes`() {
        val early = TimelineEngine.build(DayTemplates.morning(), Anchor.StartAt(at(4, 30)))
        val later = TimelineEngine.build(DayTemplates.morning(), Anchor.StartAt(at(5, 0)))

        assertEquals(early.blocks.size, later.blocks.size)
        early.blocks.zip(later.blocks).forEach { (before, after) ->
            assertEquals(before.block.id, after.block.id)
            assertEquals(before.start.plusMinutes(30), after.start)
            assertEquals(before.minutes, after.minutes)
        }
        assertEquals(at(6, 45), later.end)
    }

    @Test
    fun `finish-by works backwards from the deadline`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(), Anchor.FinishBy(at(6, 15)))

        assertEquals(at(4, 30), timeline.start)
        assertEquals(at(6, 15), timeline.end)
    }

    @Test
    fun `evening chain finishing at bedtime starts the wind-down at 1945`() {
        val timeline = TimelineEngine.build(DayTemplates.evening(), Anchor.FinishBy(DayTemplates.DEFAULT_BEDTIME))

        assertEquals(at(19, 45), timeline.block("wind_down")?.start)
        assertEquals(at(20, 15), timeline.block("prepare_sleep")?.start)
        assertEquals(at(20, 30), timeline.end)
    }

    @Test
    fun `a roomy window leaves everything at full length`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(), Anchor.Window(at(4, 30), at(6, 30)))

        assertFalse(timeline.wasCompressed)
        assertEquals(105, timeline.totalMinutes)
    }

    @Test
    fun `a forty minute window keeps warm-up and cool-down and drops the meal first`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(), Anchor.Window(at(5, 0), at(5, 40)))

        assertTrue(timeline.totalMinutes <= 40, "used ${timeline.totalMinutes}′ of a 40′ window")
        assertTrue(timeline.block("warm_up") != null, "the warm-up must never be dropped")
        assertTrue(timeline.block("cool_down") != null, "the cool-down must never be dropped")
        assertEquals(listOf("breakfast"), timeline.dropped.map { it.id })
        assertEquals(0, timeline.overflowMinutes)
        assertEquals(at(5, 0), timeline.start)
    }

    @Test
    fun `the ten minute case keeps only the essentials and reports the overflow`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(), Anchor.Window(at(5, 0), at(5, 10)))

        assertTrue(timeline.block("warm_up") != null)
        assertTrue(timeline.block("cool_down") != null)
        assertTrue(timeline.dropped.map { it.id }.containsAll(listOf("breakfast", "shower", "main")))
        // Floor of the three essentials is 5 + 6 + 5 = 16′, so a 10′ window overflows by 6.
        assertEquals(6, timeline.overflowMinutes)
    }

    @Test
    fun `surplus minutes go to the highest priority block first`() {
        val blocks = listOf(
            DayBlock("essential", "E", BlockKind.WARM_UP, minutes = 12, minMinutes = 6, priority = Priority.ESSENTIAL),
            DayBlock("normal", "N", BlockKind.MEAL, minutes = 20, minMinutes = 10, priority = Priority.NORMAL),
        )
        // Floor is 16′; a 20′ window has 4 spare minutes to hand back.
        val timeline = TimelineEngine.build(blocks, Anchor.Window(at(6, 0), at(6, 20)))

        assertEquals(10, timeline.block("essential")?.minutes, "the essential block should be topped up first")
        assertEquals(10, timeline.block("normal")?.minutes)
    }

    @Test
    fun `a chain running past midnight still measures correctly`() {
        val blocks = listOf(
            DayBlock("late", "L", BlockKind.WIND_DOWN, minutes = 40),
        )
        val timeline = TimelineEngine.build(blocks, Anchor.StartAt(at(23, 50)))

        assertEquals(at(0, 30), timeline.end)
    }

    @Test
    fun `duplicate block ids are rejected`() {
        val blocks = listOf(
            DayBlock("same", "A", BlockKind.MAIN, 10),
            DayBlock("same", "B", BlockKind.MAIN, 10),
        )
        val failure = runCatching { TimelineEngine.build(blocks, Anchor.StartAt(at(5, 0))) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException, "expected the engine to reject duplicate ids")
    }

    @Test
    fun `a shorter main session shortens the day without moving the start`() {
        val timeline = TimelineEngine.build(DayTemplates.morning(mainMinutes = 20), Anchor.StartAt(at(4, 30)))

        assertEquals(at(4, 30), timeline.start)
        assertEquals(at(6, 2), timeline.end)
    }
}
