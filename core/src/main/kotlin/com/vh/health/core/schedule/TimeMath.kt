package com.vh.health.core.schedule

import java.time.LocalTime

internal const val MINUTES_PER_DAY: Int = 24 * 60

/**
 * Minutes from [from] to [to], wrapping past midnight so an evening chain that
 * runs into the next day still measures correctly. Equal times read as zero.
 */
internal fun minutesBetween(from: LocalTime, to: LocalTime): Int {
    val diff = to.toSecondOfDay() / 60 - from.toSecondOfDay() / 60
    return if (diff >= 0) diff else diff + MINUTES_PER_DAY
}
