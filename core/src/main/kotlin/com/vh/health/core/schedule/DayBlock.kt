package com.vh.health.core.schedule

import java.time.LocalTime

enum class BlockKind {
    PERSONAL_CARE,
    WARM_UP,
    MAIN,
    KNEE,
    COOL_DOWN,
    MEAL,
    WIND_DOWN,
    BUFFER,
}

/**
 * How hard the block fights to stay when the day has to be squeezed.
 *
 * [ESSENTIAL] blocks are never dropped — they can only shrink to their floor. The
 * warm-up and the cool-down sit here on purpose: they are what protects the knees
 * on a body that has been asleep for eight hours.
 */
enum class Priority { ESSENTIAL, HIGH, NORMAL, OPTIONAL }

/**
 * One stretch of the routine. Carries a duration, never a time of day.
 *
 * @param minutes what the block wants when the day is roomy.
 * @param minMinutes the shortest it may be squeezed to before it is dropped instead.
 */
data class DayBlock(
    val id: String,
    val title: String,
    val kind: BlockKind,
    val minutes: Int,
    val minMinutes: Int = minutes,
    val priority: Priority = Priority.NORMAL,
    val note: String? = null,
) {
    init {
        require(id.isNotBlank()) { "a block needs an id" }
        require(minutes > 0) { "block '$id' must last at least a minute" }
        require(minMinutes in 1..minutes) {
            "block '$id' has an impossible floor: ${minMinutes}′ of ${minutes}′"
        }
    }
}

/** A [DayBlock] once the anchor has given it a real start time and a real length. */
data class ScheduledBlock(
    val block: DayBlock,
    val start: LocalTime,
    val minutes: Int,
) {
    val end: LocalTime get() = start.plusMinutes(minutes.toLong())

    /** True when the day was tight enough that this block had to give up time. */
    val isShortened: Boolean get() = minutes < block.minutes
}

/** The result of pinning a list of blocks to an [Anchor]. */
data class Timeline(
    val blocks: List<ScheduledBlock>,
    val dropped: List<DayBlock> = emptyList(),
    val overflowMinutes: Int = 0,
) {
    val start: LocalTime? get() = blocks.firstOrNull()?.start
    val end: LocalTime? get() = blocks.lastOrNull()?.end
    val totalMinutes: Int get() = blocks.sumOf { it.minutes }
    val wasCompressed: Boolean get() = dropped.isNotEmpty() || blocks.any { it.isShortened }

    fun block(id: String): ScheduledBlock? = blocks.firstOrNull { it.block.id == id }
}
