package com.vh.health.core

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.content.Weekday
import com.vh.health.core.content.WorkoutBlock
import com.vh.health.core.content.WorkoutItem
import com.vh.health.core.content.Workout
import com.vh.health.core.session.SessionBuilder
import com.vh.health.core.session.StepPhase
import com.vh.health.core.session.cursorAt
import com.vh.health.core.session.totalSeconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionBuilderTest {

    private val tabata = Workout(
        id = "t", titleVi = "Tabata", focusVi = "", minutes = 4, rpe = "8",
        blocks = listOf(
            WorkoutBlock(
                titleVi = "Vong 1",
                rounds = 2,
                restBetweenRoundsSeconds = 15,
                restAfterSeconds = 30,
                items = listOf(
                    WorkoutItem(exerciseId = "a", workSeconds = 20, restSeconds = 10),
                    WorkoutItem(exerciseId = "b", workSeconds = 20, restSeconds = 10),
                ),
            ),
            WorkoutBlock(
                titleVi = "Vong 2",
                rounds = 1,
                restAfterSeconds = 20,
                items = listOf(WorkoutItem(exerciseId = "c", workSeconds = 30)),
            ),
        ),
    )

    @Test
    fun `built steps sum to exactly the workout's estimated seconds, prepare aside`() {
        val steps = SessionBuilder.build(tabata)
        val withoutPrepare = steps.drop(1) // first step is always PREPARE
        assertEquals(tabata.estimatedSeconds, withoutPrepare.sumOf { it.seconds })
    }

    @Test
    fun `the session opens with a prepare step naming the first exercise`() {
        val steps = SessionBuilder.build(tabata)
        assertEquals(StepPhase.PREPARE, steps.first().phase)
        assertEquals("a", steps.first().exerciseId)
        assertEquals(SessionBuilder.PREPARE_SECONDS, steps.first().seconds)
    }

    @Test
    fun `round one alternates work and rest between the two items`() {
        val steps = SessionBuilder.build(tabata)
        val round1 = steps.drop(1).take(4)
        assertEquals(
            listOf(
                StepPhase.WORK to "a",
                StepPhase.REST to "b",
                StepPhase.WORK to "b",
                StepPhase.REST to "a",
            ),
            round1.map { it.phase to it.exerciseId },
        )
    }

    @Test
    fun `rest between rounds previews the first exercise of the next round`() {
        val steps = SessionBuilder.build(tabata)
        val betweenRounds = steps.first { it.seconds == 15 && it.phase == StepPhase.REST }
        assertEquals("a", betweenRounds.exerciseId)
        assertEquals(2, betweenRounds.round)
    }

    @Test
    fun `rest after the block previews the first exercise of the next block`() {
        val steps = SessionBuilder.build(tabata)
        val afterBlock = steps.first { it.seconds == 30 && it.phase == StepPhase.REST }
        assertEquals("c", afterBlock.exerciseId)
    }

    @Test
    fun `the very last step is the block's trailing rest and previews nothing`() {
        val steps = SessionBuilder.build(tabata)
        assertEquals(StepPhase.REST, steps.last().phase)
        assertEquals(null, steps.last().exerciseId)
        assertEquals(20, steps.last().seconds)
    }

    @Test
    fun `new block is flagged only when transitioning into a later block, not by prepare`() {
        val steps = SessionBuilder.build(tabata)
        assertFalse(steps.first().isNewBlock, "prepare already announces the first block")
        assertFalse(steps[1].isNewBlock, "the first block's own first step isn't a transition")
        assertEquals(1, steps.count { it.isNewBlock })
        assertTrue(steps.first { it.blockTitle == "Vong 2" }.isNewBlock)
    }

    @Test
    fun `a workout with a single untimed rest-free item still builds`() {
        val minimal = Workout(
            id = "m", titleVi = "M", focusVi = "", minutes = 1, rpe = "5",
            blocks = listOf(WorkoutBlock(titleVi = "B", items = listOf(WorkoutItem(exerciseId = "x", workSeconds = 45)))),
        )
        val steps = SessionBuilder.build(minimal)
        assertEquals(2, steps.size) // prepare + one work step
        assertEquals(45, steps.last().seconds)
    }

    @Test
    fun `cursorAt walks the timeline and reports the right remaining seconds`() {
        val steps = SessionBuilder.build(tabata)
        // step 0 = prepare(10), step 1 = work "a"(20)
        val duringPrepare = steps.cursorAt(5)
        assertEquals(0, duringPrepare.stepIndex)
        assertEquals(5, duringPrepare.remainingInStep)

        val atBoundary = steps.cursorAt(10) // exactly when prepare ends
        assertEquals(1, atBoundary.stepIndex)
        assertEquals(20, atBoundary.remainingInStep)

        val intoWork = steps.cursorAt(25) // 15s into the first WORK
        assertEquals(1, intoWork.stepIndex)
        assertEquals(5, intoWork.remainingInStep)
    }

    @Test
    fun `cursorAt past the end reports finished rather than crashing`() {
        val steps = SessionBuilder.build(tabata)
        val past = steps.cursorAt(steps.totalSeconds() + 100)
        assertTrue(past.finished)
        assertEquals(steps.lastIndex, past.stepIndex)
    }

    @Test
    fun `an empty step list is finished immediately`() {
        assertTrue(emptyList<com.vh.health.core.session.SessionStep>().cursorAt(0).finished)
    }

    @Test
    fun `isCountIn lights up only in the last three seconds, never at zero`() {
        val steps = SessionBuilder.build(tabata)
        assertFalse(steps.cursorAt(6).isCountIn)  // 4s left of prepare
        assertTrue(steps.cursorAt(7).isCountIn)   // 3s left
        assertTrue(steps.cursorAt(9).isCountIn)   // 1s left
        assertFalse(steps.cursorAt(10).isCountIn) // moved into the next step
    }

    @Test
    fun `every bundled workout builds a session whose total matches its estimate`() {
        val content = ContentLoader.loadProgram()
        content.workouts.forEach { workout ->
            val steps = SessionBuilder.build(workout)
            val playSeconds = steps.drop(1).sumOf { it.seconds }
            assertEquals(
                workout.estimatedSeconds, playSeconds,
                "${workout.id}: session build diverges from estimatedSeconds",
            )
        }
    }

    @Test
    fun `every day of the week resolves to a workout that builds a non-empty session`() {
        val content = ContentLoader.loadProgram()
        Weekday.entries.forEach { day ->
            val workout = content.workoutFor(day)
            assertTrue(workout != null, "no workout for ${day.labelVi}")
            assertTrue(SessionBuilder.build(workout!!).size > 1, "${day.labelVi} builds an empty session")
        }
    }
}
