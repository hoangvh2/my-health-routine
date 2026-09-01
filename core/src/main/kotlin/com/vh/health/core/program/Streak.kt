package com.vh.health.core.program

import java.time.LocalDate

/**
 * How many days in a row, counting back from [today], have a completed session —
 * the number the Progress screen shows as "chuỗi ngày". A rest day the plan itself
 * calls for is not a broken streak; only a scheduled day with no logged session
 * breaks it. This function has no concept of which days are rest days — that lives
 * in the Program — so [sessionDates] should already exclude them.
 *
 * Today not having a session yet does not zero the streak: the day isn't over. The
 * count starts from today if today is already done, otherwise from yesterday, so a
 * user checking the app at 10am mid-streak sees yesterday's count, not a demoralising
 * zero for a workout they haven't had the chance to do yet.
 */
fun currentStreak(sessionDates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    var day = if (today in sessionDates) today else today.minusDays(1)
    var streak = 0
    while (day in sessionDates) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}
