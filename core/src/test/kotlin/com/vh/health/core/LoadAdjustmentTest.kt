package com.vh.health.core

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.content.Equipment
import com.vh.health.core.content.Exercise
import com.vh.health.core.content.ExerciseLibrary
import com.vh.health.core.content.MuscleGroup
import com.vh.health.core.content.Weekday
import com.vh.health.core.content.Workout
import com.vh.health.core.content.WorkoutBlock
import com.vh.health.core.content.WorkoutItem
import com.vh.health.core.program.KneeLoadPolicy
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.program.applyCardioLoadFactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoadAdjustmentTest {

    private val library = ContentLoader.loadLibrary()
    private val program = ContentLoader.loadProgram()

    @Test
    fun `exactly three bundled workouts track a knee signal — the running-heavy days`() {
        val flagged = program.workouts.filter { it.tracksKneeSignal }.map { it.id }.toSet()
        assertEquals(setOf("w_zone2_knee", "w_intervals_core", "w_long_easy"), flagged)
    }

    @Test
    fun `a factor of exactly one returns the same workout instance, no rounding risk`() {
        val workout = program.workout("w_zone2_knee")!!
        assertTrue(workout === applyCardioLoadFactor(workout, 1.0, library))
    }

    @Test
    fun `an overloaded knee shrinks cardio time and leaves the knee-prehab block untouched`() {
        val workout = program.workout("w_zone2_knee")!!
        val decision = KneeLoadPolicy.decide(KneeSignal.OVERLOADED)
        val adjusted = applyCardioLoadFactor(workout, decision.impactFactor, library)

        val originalCardioSeconds = workout.blocks.flatMap { it.items }
            .filter { library[it.exerciseId]?.group == MuscleGroup.CARDIO }
            .sumOf { it.workSeconds + it.restSeconds }
        val adjustedCardioSeconds = adjusted.blocks.flatMap { it.items }
            .filter { library[it.exerciseId]?.group == MuscleGroup.CARDIO }
            .sumOf { it.workSeconds + it.restSeconds }

        assertTrue(adjustedCardioSeconds < originalCardioSeconds, "cardio time should shrink on an overloaded signal")

        // Every non-cardio item (the knee-prehab block) is byte-for-byte unchanged.
        val originalKneeItems = workout.blocks.flatMap { it.items }.filter { library[it.exerciseId]?.group != MuscleGroup.CARDIO }
        val adjustedKneeItems = adjusted.blocks.flatMap { it.items }.filter { library[it.exerciseId]?.group != MuscleGroup.CARDIO }
        assertEquals(originalKneeItems, adjustedKneeItems, "strength/prehab work must never be scaled back — D-007")
    }

    @Test
    fun `a clear knee signal grows cardio time by the same ten percent RunVolumeGuard allows`() {
        val workout = program.workout("w_long_easy")!!
        val decision = KneeLoadPolicy.decide(KneeSignal.CLEAR)
        val adjusted = applyCardioLoadFactor(workout, decision.impactFactor, library)
        assertTrue(adjusted.estimatedSeconds > workout.estimatedSeconds)
    }

    @Test
    fun `a workout with no cardio items at all is unaffected by any factor`() {
        val strength = program.workout("w_strength_full_a")!!
        val adjusted = applyCardioLoadFactor(strength, 0.7, library)
        assertEquals(strength, adjusted)
    }

    @Test
    fun `scaling never drives a real item's time to zero or negative`() {
        val tiny = Workout(
            id = "t", titleVi = "T", focusVi = "", minutes = 1, rpe = "5",
            blocks = listOf(
                WorkoutBlock(titleVi = "B", items = listOf(WorkoutItem(exerciseId = "run", workSeconds = 6, restSeconds = 6))),
            ),
        )
        val miniLibrary = ExerciseLibrary(
            version = 1,
            exercises = listOf(Exercise(id = "run", nameVi = "Chạy", nameEn = "Run", group = MuscleGroup.CARDIO, equipment = listOf(Equipment.NONE))),
        )
        val adjusted = applyCardioLoadFactor(tiny, 0.1, miniLibrary)
        val item = adjusted.blocks.first().items.first()
        assertTrue(item.workSeconds >= 1, "must never floor to zero or below")
        assertTrue(item.restSeconds >= 1)
    }

    @Test
    fun `every knee-tracking workout still builds a sane session after being scaled either direction`() {
        val flagged = program.workouts.filter { it.tracksKneeSignal }
        listOf(KneeSignal.CLEAR, KneeSignal.OVERLOADED).forEach { signal ->
            val factor = KneeLoadPolicy.decide(signal).impactFactor
            flagged.forEach { workout ->
                val adjusted = applyCardioLoadFactor(workout, factor, library)
                assertTrue(adjusted.estimatedSeconds > 0, "${workout.id} under $signal must still be a real workout")
            }
        }
    }

    @Test
    fun `the week still resolves a workout for every weekday after this content change`() {
        Weekday.entries.forEach { day -> assertTrue(program.workoutFor(day) != null, "no workout for $day") }
    }
}
