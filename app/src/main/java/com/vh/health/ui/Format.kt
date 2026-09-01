package com.vh.health.ui

import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.hhmm(): String = format(HourMinute)

/** "1 giờ 45", "45 phút" — how a person would say it out loud. */
fun minutesAsText(minutes: Int): String = when {
    minutes < 60 -> "$minutes phút"
    minutes % 60 == 0 -> "${minutes / 60} giờ"
    else -> "${minutes / 60} giờ ${minutes % 60}"
}
