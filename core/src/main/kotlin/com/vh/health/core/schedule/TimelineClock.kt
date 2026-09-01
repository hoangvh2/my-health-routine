package com.vh.health.core.schedule

import java.time.LocalTime

enum class DayPhase { BEFORE, DURING, AFTER }

enum class BlockState { PAST, CURRENT, UPCOMING }

data class TimelinePosition(
    val phase: DayPhase,
    /** 0f before the day starts, 0..1 while it runs, 1f once it is over. */
    val progress: Float,
    val currentBlockId: String?,
    val minutesUntilStart: Int,
    val minutesRemaining: Int,
)

/**
 * Where [now] falls inside this timeline.
 *
 * A [LocalTime] carries no date, so "before" and "after" would otherwise be
 * ambiguous: 20:00 sits both 15½ hours after a 04:30 start and 8½ hours before the
 * next one. [lookAheadHours] settles it — a start that close ahead means tomorrow's
 * morning has not happened yet, not that today's is long gone.
 */
fun Timeline.positionAt(now: LocalTime, lookAheadHours: Int = 12): TimelinePosition {
    val dayStart = start
        ?: return TimelinePosition(DayPhase.BEFORE, 0f, null, 0, 0)
    val total = totalMinutes
    val elapsed = minutesBetween(dayStart, now)

    if (total > 0 && elapsed < total) {
        val current = blocks.firstOrNull { scheduled ->
            val offset = minutesBetween(dayStart, scheduled.start)
            elapsed >= offset && elapsed < offset + scheduled.minutes
        }
        return TimelinePosition(
            phase = DayPhase.DURING,
            progress = elapsed.toFloat() / total,
            currentBlockId = current?.block?.id,
            minutesUntilStart = 0,
            minutesRemaining = total - elapsed,
        )
    }

    val untilNextStart = minutesBetween(now, dayStart)
    return if (untilNextStart <= lookAheadHours * 60) {
        TimelinePosition(DayPhase.BEFORE, 0f, null, untilNextStart, total)
    } else {
        TimelinePosition(DayPhase.AFTER, 1f, null, 0, 0)
    }
}

/** Past / current / upcoming for every block, keyed by block id. */
fun Timeline.blockStates(now: LocalTime, lookAheadHours: Int = 12): Map<String, BlockState> {
    val position = positionAt(now, lookAheadHours)
    return when (position.phase) {
        DayPhase.BEFORE -> blocks.associate { it.block.id to BlockState.UPCOMING }
        DayPhase.AFTER -> blocks.associate { it.block.id to BlockState.PAST }
        DayPhase.DURING -> {
            val dayStart = blocks.first().start
            val elapsed = minutesBetween(dayStart, now)
            blocks.associate { scheduled ->
                val offset = minutesBetween(dayStart, scheduled.start)
                scheduled.block.id to when {
                    elapsed >= offset + scheduled.minutes -> BlockState.PAST
                    elapsed >= offset -> BlockState.CURRENT
                    else -> BlockState.UPCOMING
                }
            }
        }
    }
}
