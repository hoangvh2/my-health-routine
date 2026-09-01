package com.vh.health.core.program

import com.vh.health.core.content.ExerciseLibrary
import com.vh.health.core.content.MuscleGroup
import com.vh.health.core.content.Workout
import kotlin.math.roundToInt

private const val MIN_SCALED_SECONDS = 5

/**
 * Applies a knee-load decision (see [KneeLoadPolicy]) to a workout's cardio content —
 * this is what turns the traffic light from a number nobody sees into a workout that
 * is actually shorter or longer next week.
 *
 * Only items whose exercise belongs to [MuscleGroup.CARDIO] are touched: strength,
 * knee-prehab and core work are returned exactly as authored, matching D-007's rule
 * that strength load is never cut back — only impact volume gives way. Both a cardio
 * item's work time and its rest/walk-recovery time scale together, so a running
 * interval's own run:walk ratio holds while the whole session's total locomotion
 * time moves by [factor].
 *
 * A factor of exactly 1.0 returns [workout] itself, unmodified — a CLEAR knee week
 * never risks quietly rounding a workout into something subtly different from what
 * was authored and tested.
 */
fun applyCardioLoadFactor(workout: Workout, factor: Double, library: ExerciseLibrary): Workout {
    if (factor == 1.0) return workout
    require(factor > 0) { "a load factor must be positive, got $factor" }

    fun scale(seconds: Int): Int =
        if (seconds <= 0) seconds else (seconds * factor).roundToInt().coerceAtLeast(MIN_SCALED_SECONDS)

    return workout.copy(
        blocks = workout.blocks.map { block ->
            block.copy(
                items = block.items.map { item ->
                    val isCardio = library[item.exerciseId]?.group == MuscleGroup.CARDIO
                    if (!isCardio) item else item.copy(workSeconds = scale(item.workSeconds), restSeconds = scale(item.restSeconds))
                },
            )
        },
    )
}
