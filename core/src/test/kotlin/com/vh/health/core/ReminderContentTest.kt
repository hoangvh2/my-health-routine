package com.vh.health.core

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.notify.ReminderContent
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReminderContentTest {

    private val program = ContentLoader.loadProgram()
    private fun at(h: Int, m: Int) = LocalTime.of(h, m)

    @Test
    fun `morning copy for a real workout leads with the clock time and names the workout`() {
        val workout = program.workout("w_zone2_knee")!!
        val copy = ReminderContent.forMorningStart(at(4, 30), workout)

        assertTrue(copy.title.startsWith("04:30"))
        assertTrue(copy.title.contains(workout.titleVi))
        assertTrue(copy.text.contains("${workout.minutes}"), "expected the duration in the body")
        assertTrue(copy.text.contains(workout.rpe), "expected RPE in the body")
    }

    @Test
    fun `morning copy for a rest day does not claim there is a workout`() {
        val copy = ReminderContent.forMorningStart(at(4, 30), workout = null)
        assertTrue(copy.title.contains("dậy"))
        assertEquals(null, copy.bigText)
    }

    @Test
    fun `every bundled workout produces sane morning copy — no crashes, no blank fields`() {
        program.workouts.forEach { workout ->
            val copy = ReminderContent.forMorningStart(at(4, 30), workout)
            assertTrue(copy.title.isNotBlank())
            assertTrue(copy.text.isNotBlank())
        }
    }

    @Test
    fun `the three desk break cues are genuinely different, not the same line three times`() {
        val texts = (0..2).map { ReminderContent.forDeskBreak(it, at(9, 30)).text }
        assertEquals(3, texts.toSet().size)
    }

    @Test
    fun `an out-of-range desk break index falls back rather than crashing`() {
        val copy = ReminderContent.forDeskBreak(99, at(9, 30))
        assertTrue(copy.text.isNotBlank())
    }

    @Test
    fun `evening copy states both the reminder time and the actual bedtime`() {
        val copy = ReminderContent.forEveningWindDown(at(19, 45), at(20, 30))
        assertTrue(copy.title.startsWith("19:45"))
        assertTrue(copy.text.contains("20:30"))
    }
}
