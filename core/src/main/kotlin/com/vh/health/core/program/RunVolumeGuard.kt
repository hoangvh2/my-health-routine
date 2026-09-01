package com.vh.health.core.program

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The hard ceiling on how fast walking and running volume may grow.
 *
 * Enforced rather than suggested: the ache this programme is built around comes from
 * volume, and the day the user feels good is exactly the day the cap earns its keep.
 */
object RunVolumeGuard {

    fun cap(previousWeekMinutes: Int, proposedMinutes: Int, factor: Double = 1.0 + KneeLoadPolicy.MAX_WEEKLY_IMPACT_INCREASE): Int {
        require(previousWeekMinutes >= 0) { "last week cannot be negative" }
        require(proposedMinutes >= 0) { "proposed volume cannot be negative" }
        if (previousWeekMinutes == 0) return proposedMinutes
        val ceiling = floor(previousWeekMinutes * factor).toInt()
        return minOf(proposedMinutes, ceiling)
    }

    fun exceedsCap(previousWeekMinutes: Int, proposedMinutes: Int): Boolean =
        proposedMinutes > cap(previousWeekMinutes, proposedMinutes)

    /** Applies a [LoadDecision] to last week's volume. */
    fun nextWeek(previousWeekMinutes: Int, decision: LoadDecision): Int =
        (previousWeekMinutes * decision.impactFactor).roundToInt()
}
