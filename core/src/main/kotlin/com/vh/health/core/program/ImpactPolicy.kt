package com.vh.health.core.program

/**
 * When bouncing is allowed back in.
 *
 * The knees here are not injured — they are under-conditioned from sitting, and ache
 * only at higher walking or running volume. Avoiding impact forever would leave the
 * tendons unprepared for running, so impact comes back deliberately: the first two
 * weeks build a strength base, then small in-place hops start teaching the tendons
 * to handle bounce, well before running volume climbs.
 */
enum class ImpactLevel { NONE, LOW, MODERATE }

object ImpactPolicy {

    const val PLYOMETRICS_FROM_WEEK: Int = 3

    fun allowedLevel(week: Int): ImpactLevel {
        require(week >= 1) { "weeks are counted from 1, got $week" }
        return when {
            week < PLYOMETRICS_FROM_WEEK -> ImpactLevel.LOW
            week < PLYOMETRICS_FROM_WEEK + 4 -> ImpactLevel.MODERATE
            else -> ImpactLevel.MODERATE
        }
    }

    /** True when an exercise tagged with [exerciseImpact] may appear in [week]. */
    fun allows(week: Int, exerciseImpact: ImpactLevel): Boolean =
        exerciseImpact.ordinal <= allowedLevel(week).ordinal
}
