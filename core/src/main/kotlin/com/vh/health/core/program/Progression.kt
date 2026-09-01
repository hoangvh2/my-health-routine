package com.vh.health.core.program

import java.time.LocalDate
import kotlin.math.roundToInt

/** Where a week sits inside the repeating four-week block. */
enum class Phase(val labelVi: String, val volumeFactor: Double, val rpeLow: Int, val rpeHigh: Int) {
    ADAPT("Làm quen", 1.00, 5, 6),
    BUILD("Tăng tải", 1.10, 6, 7),
    PEAK("Đỉnh khối", 1.20, 7, 8),
    DELOAD("Giảm tải", 0.60, 4, 5),
}

/**
 * Four-week blocks: build for three weeks, then take 40% off so the body can
 * actually absorb the work. Week 4 is where progress is made, not week 3.
 */
object Progression {

    const val WEEKS_PER_BLOCK: Int = 4

    /** @param week 1-based, counted from the first week of the whole programme. */
    fun phaseOf(week: Int): Phase {
        require(week >= 1) { "weeks are counted from 1, got $week" }
        return Phase.entries[(week - 1) % WEEKS_PER_BLOCK]
    }

    fun blockOf(week: Int): Int {
        require(week >= 1) { "weeks are counted from 1, got $week" }
        return (week - 1) / WEEKS_PER_BLOCK + 1
    }

    fun weekWithinBlock(week: Int): Int {
        require(week >= 1) { "weeks are counted from 1, got $week" }
        return (week - 1) % WEEKS_PER_BLOCK + 1
    }

    /** Scales a baseline set/round count for the phase the given week falls in. */
    fun scaleVolume(baseline: Int, week: Int): Int =
        (baseline * phaseOf(week).volumeFactor).roundToInt().coerceAtLeast(1)

    /**
     * Which 1-based programme week [todayEpochDay] falls in, given the day the
     * programme started. The single source of this arithmetic — Today and Schedule
     * both call it, so they can never quietly disagree about what week it is.
     *
     * A start day that is still in the future (clock changed, bad data) reads as
     * week 1 rather than a nonsensical zero or negative week.
     */
    fun weekNumber(programStartEpochDay: Long, todayEpochDay: Long = LocalDate.now().toEpochDay()): Int {
        val elapsedDays = todayEpochDay - programStartEpochDay
        return (elapsedDays / 7 + 1).coerceAtLeast(1).toInt()
    }
}
