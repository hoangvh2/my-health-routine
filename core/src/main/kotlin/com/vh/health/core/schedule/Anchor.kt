package com.vh.health.core.schedule

import java.time.LocalTime

/**
 * Where a day's chain of blocks is pinned to the clock.
 *
 * Nothing in the app stores a wall-clock time for a block. A block knows only how
 * long it lasts; the anchor is what turns a list of blocks into real times. Move the
 * anchor and every downstream time moves with it.
 */
sealed interface Anchor {

    /** Lay the blocks out forward from [time]. The everyday case. */
    data class StartAt(val time: LocalTime) : Anchor

    /** Work backwards so the last block ends exactly at [time]. */
    data class FinishBy(val time: LocalTime) : Anchor

    /**
     * Fit the blocks between [from] and [to], compressing them if they do not fit.
     * This is what "sáng nay chỉ có 40 phút" turns into.
     */
    data class Window(val from: LocalTime, val to: LocalTime) : Anchor
}
