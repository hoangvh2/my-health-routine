package com.vh.health.core

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.content.Equipment
import com.vh.health.core.content.MuscleGroup
import com.vh.health.core.content.Weekday
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs against the real bundled content, so a typo in exercises.json or program.json
 * fails CI rather than crashing the session player at five in the morning.
 */
class ContentTest {

    private val library = ContentLoader.loadLibrary()
    private val program = ContentLoader.loadProgram()

    @Test
    fun `bundled content is internally consistent`() {
        val problems = ContentLoader.validate(library, program)
        assertTrue(problems.isEmpty(), "content problems:\n" + problems.joinToString("\n"))
    }

    @Test
    fun `every group the programme needs is populated`() {
        MuscleGroup.entries.forEach { group ->
            assertTrue(library.inGroup(group).isNotEmpty(), "no exercises in ${group.name}")
        }
        assertTrue(library.exercises.size >= 60, "library has only ${library.exercises.size} exercises")
    }

    @Test
    fun `every exercise carries the coaching text the library page promises`() {
        library.exercises.forEach { exercise ->
            assertTrue(exercise.cues.size >= 3, "${exercise.id} has ${exercise.cues.size} cues")
            assertTrue(exercise.mistakes.size >= 3, "${exercise.id} has ${exercise.mistakes.size} mistakes")
            assertTrue(exercise.nameVi.isNotBlank(), "${exercise.id} has no Vietnamese name")
            assertTrue(exercise.animation != null, "${exercise.id} has no animation key")
        }
    }

    @Test
    fun `the knee work the programme is built around is actually present`() {
        val kneeWork = library.kneeWork().map { it.id }
        listOf(
            "kn_spanish_squat",
            "kn_step_down",
            "kn_tibialis_raise",
            "kn_calf_raise_seated",
            "kn_calf_raise_standing",
            "kn_monster_walk",
        ).forEach { assertTrue(it in kneeWork, "$it missing from the knee-focused set") }
    }

    @Test
    fun `the week covers all seven days and names a workout for each`() {
        assertEquals(7, program.week.size)
        Weekday.entries.forEach { day ->
            assertTrue(program.workoutFor(day) != null, "no workout for ${day.labelVi}")
        }
    }

    @Test
    fun `each workout actually lasts roughly what it claims`() {
        program.workouts.forEach { workout ->
            val actualMinutes = workout.estimatedSeconds / 60.0
            assertTrue(
                abs(actualMinutes - workout.minutes) <= 4.0,
                "${workout.id} claims ${workout.minutes}′ but its blocks add up to ${"%.1f".format(actualMinutes)}′",
            )
        }
    }

    @Test
    fun `a bodyweight-only user still has a usable library`() {
        val bodyweight = library.requiringOnly(setOf(Equipment.MAT))
        assertTrue(bodyweight.size >= 20, "only ${bodyweight.size} exercises need nothing but a mat")
    }

    @Test
    fun `unused exercises are the substitution pool, not orphans`() {
        // Not an error: these back the harder/easier swaps and later programme blocks.
        // The assertion guards the other direction — the pool must not swallow the library.
        val unused = ContentLoader.unusedExercises(library, program)
        assertTrue(
            unused.size < library.exercises.size / 2,
            "over half the library is unused by the programme: ${unused.size} of ${library.exercises.size}",
        )
    }
}
