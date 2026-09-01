package com.vh.health.core.session

import com.vh.health.core.content.Workout

/**
 * Flattens a [Workout] into the ordered list of [SessionStep]s a player ticks
 * through. This is the one place that interprets rounds, rest-between-rounds and
 * rest-after — get it right here and the player itself stays a dumb clock.
 *
 * The seconds of every WORK and REST step this produces sum to exactly
 * [Workout.estimatedSeconds] — SessionBuilderTest checks that against the real
 * bundled programme, so a mistake here fails `:core:test`, not a 5am session.
 */
object SessionBuilder {

    /** Still screen before the first rep, so the user has time to get into position. */
    const val PREPARE_SECONDS = 10

    fun build(workout: Workout): List<SessionStep> {
        val steps = mutableListOf<SessionStep>()
        val firstBlock = workout.blocks.firstOrNull()
        if (firstBlock != null) {
            steps += SessionStep(
                phase = StepPhase.PREPARE,
                exerciseId = firstBlock.items.firstOrNull()?.exerciseId,
                seconds = PREPARE_SECONDS,
                blockTitle = firstBlock.titleVi,
                round = 1,
                totalRounds = firstBlock.rounds,
                // PREPARE already announces the session's first block; isNewBlock is
                // reserved for transitions the PREPARE screen didn't cover.
                isNewBlock = false,
            )
        }

        workout.blocks.forEachIndexed { blockIndex, block ->
            for (round in 1..block.rounds) {
                block.items.forEachIndexed { itemIndex, item ->
                    steps += SessionStep(
                        phase = StepPhase.WORK,
                        exerciseId = item.exerciseId,
                        seconds = item.workSeconds,
                        blockTitle = block.titleVi,
                        round = round,
                        totalRounds = block.rounds,
                        // Only a transition into a *later* block counts as "new" — the
                        // very first block was already announced by PREPARE.
                        isNewBlock = round == 1 && itemIndex == 0 && blockIndex > 0,
                    )
                    if (item.restSeconds > 0) {
                        steps += SessionStep(
                            phase = StepPhase.REST,
                            exerciseId = peekNext(workout, blockIndex, round, itemIndex),
                            seconds = item.restSeconds,
                            blockTitle = block.titleVi,
                            round = round,
                            totalRounds = block.rounds,
                            isNewBlock = false,
                        )
                    }
                }
                if (round < block.rounds && block.restBetweenRoundsSeconds > 0) {
                    steps += SessionStep(
                        phase = StepPhase.REST,
                        exerciseId = block.items.firstOrNull()?.exerciseId,
                        seconds = block.restBetweenRoundsSeconds,
                        blockTitle = block.titleVi,
                        round = round + 1,
                        totalRounds = block.rounds,
                        isNewBlock = false,
                    )
                }
            }
            if (block.restAfterSeconds > 0) {
                steps += SessionStep(
                    phase = StepPhase.REST,
                    exerciseId = workout.blocks.getOrNull(blockIndex + 1)?.items?.firstOrNull()?.exerciseId,
                    seconds = block.restAfterSeconds,
                    blockTitle = block.titleVi,
                    round = block.rounds,
                    totalRounds = block.rounds,
                    isNewBlock = false,
                )
            }
        }
        return steps
    }

    /** What comes right after this WORK step — the item, round, or block after it. */
    private fun peekNext(workout: Workout, blockIndex: Int, round: Int, itemIndex: Int): String? {
        val block = workout.blocks[blockIndex]
        return when {
            itemIndex + 1 < block.items.size -> block.items[itemIndex + 1].exerciseId
            round < block.rounds -> block.items.firstOrNull()?.exerciseId
            blockIndex + 1 < workout.blocks.size -> workout.blocks[blockIndex + 1].items.firstOrNull()?.exerciseId
            else -> null
        }
    }
}

/** Total runtime of a built session, PREPARE included. */
fun List<SessionStep>.totalSeconds(): Int = sumOf { it.seconds }

/** Where [elapsedSeconds] into the session lands. Clamps to the last step past the end. */
fun List<SessionStep>.cursorAt(elapsedSeconds: Int): SessionCursor {
    if (isEmpty()) return SessionCursor(0, 0, 0, finished = true)
    var acc = 0
    for ((index, step) in withIndex()) {
        if (elapsedSeconds < acc + step.seconds) {
            val elapsedInStep = elapsedSeconds - acc
            return SessionCursor(index, step.seconds - elapsedInStep, elapsedInStep, finished = false)
        }
        acc += step.seconds
    }
    return SessionCursor(size - 1, 0, last().seconds, finished = true)
}
