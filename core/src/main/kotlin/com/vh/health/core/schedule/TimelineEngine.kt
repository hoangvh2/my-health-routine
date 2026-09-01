package com.vh.health.core.schedule

import java.time.LocalTime

/**
 * Turns a list of [DayBlock]s plus an [Anchor] into real clock times.
 *
 * This is a pure function of its inputs — no clock reads, no stored times. Every
 * alarm and notification in the app registers itself from the result, which is why
 * moving the anchor can never leave the screen showing one time while the alarm
 * fires at another.
 */
object TimelineEngine {

    fun build(blocks: List<DayBlock>, anchor: Anchor): Timeline {
        requireUniqueIds(blocks)
        if (blocks.isEmpty()) return Timeline(emptyList())

        return when (anchor) {
            is Anchor.StartAt -> layOut(blocks.fullLength(), anchor.time)

            is Anchor.FinishBy -> {
                val total = blocks.sumOf { it.minutes }
                layOut(blocks.fullLength(), anchor.time.minusMinutes(total.toLong()))
            }

            is Anchor.Window -> {
                val fitted = fit(blocks, minutesBetween(anchor.from, anchor.to))
                layOut(fitted.kept, anchor.from, fitted.dropped, fitted.overflowMinutes)
            }
        }
    }

    /**
     * The shortest the day can possibly be: every block squeezed to its floor, with
     * everything droppable dropped. Useful for telling the user what will not fit.
     */
    fun essentialFloorMinutes(blocks: List<DayBlock>): Int =
        blocks.filter { it.priority == Priority.ESSENTIAL }.sumOf { it.minMinutes }

    private fun requireUniqueIds(blocks: List<DayBlock>) {
        val duplicates = blocks.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "duplicate block ids: ${duplicates.joinToString()}" }
    }

    private fun List<DayBlock>.fullLength(): List<Allocation> = map { Allocation(it, it.minutes) }

    private fun layOut(
        items: List<Allocation>,
        start: LocalTime,
        dropped: List<DayBlock> = emptyList(),
        overflowMinutes: Int = 0,
    ): Timeline {
        var cursor = start
        val scheduled = ArrayList<ScheduledBlock>(items.size)
        for ((block, minutes) in items) {
            scheduled += ScheduledBlock(block, cursor, minutes)
            cursor = cursor.plusMinutes(minutes.toLong())
        }
        return Timeline(scheduled, dropped, overflowMinutes)
    }

    /**
     * Squeeze [blocks] into [windowMinutes].
     *
     * Two stages, in this order:
     *  1. Drop, starting from the lowest priority and from the longest block within
     *     each priority, until what remains can fit at its floor. ESSENTIAL blocks
     *     are never dropped.
     *  2. Hand the leftover minutes back out, highest priority first, so a day that
     *     is only slightly tight loses time from the breakfast rather than the
     *     warm-up.
     */
    private fun fit(blocks: List<DayBlock>, windowMinutes: Int): Fitted {
        val kept = blocks.toMutableList()
        val dropped = mutableListOf<DayBlock>()

        for (priority in listOf(Priority.OPTIONAL, Priority.NORMAL, Priority.HIGH)) {
            if (kept.sumOf { it.minMinutes } <= windowMinutes) break
            val candidates = kept.filter { it.priority == priority }.sortedByDescending { it.minutes }
            for (candidate in candidates) {
                if (kept.sumOf { it.minMinutes } <= windowMinutes) break
                kept.remove(candidate)
                dropped += candidate
            }
        }

        val floor = kept.sumOf { it.minMinutes }
        val allocated = kept.associateTo(mutableMapOf()) { it.id to it.minMinutes }
        var surplus = windowMinutes - floor

        for (priority in Priority.entries) {
            if (surplus <= 0) break
            for (block in kept.filter { it.priority == priority }) {
                if (surplus <= 0) break
                val give = minOf(block.minutes - block.minMinutes, surplus)
                allocated[block.id] = allocated.getValue(block.id) + give
                surplus -= give
            }
        }

        return Fitted(
            kept = kept.map { Allocation(it, allocated.getValue(it.id)) },
            dropped = dropped.sortedBy { blocks.indexOf(it) },
            overflowMinutes = maxOf(0, floor - windowMinutes),
        )
    }

    private data class Allocation(val block: DayBlock, val minutes: Int)

    private data class Fitted(
        val kept: List<Allocation>,
        val dropped: List<DayBlock>,
        val overflowMinutes: Int,
    )
}
