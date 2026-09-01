package com.vh.health.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.vh.health.core.schedule.ReminderKind

/**
 * Three channels rather than one: each has its own Android system setting, so the
 * user can mute desk-break pings from system settings while keeping the morning
 * and evening ones — without touching this app's own settings at all.
 */
object NotificationChannels {
    const val MORNING = "morning_start"
    const val DESK_BREAK = "desk_break"
    const val EVENING = "evening_wind_down"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(MORNING, "Buổi tập sáng", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nhắc bắt đầu buổi tập mỗi sáng"
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(DESK_BREAK, "Nghỉ bàn giấy", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ba lần nhắc đứng dậy trong giờ làm việc"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(EVENING, "Hạ nhiệt buổi tối", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Nhắc giãn cơ và thở trước khi ngủ"
            },
        )
    }

    fun channelFor(kind: ReminderKind): String = when (kind) {
        ReminderKind.MORNING_START -> MORNING
        ReminderKind.DESK_BREAK_1, ReminderKind.DESK_BREAK_2, ReminderKind.DESK_BREAK_3 -> DESK_BREAK
        ReminderKind.EVENING_WIND_DOWN -> EVENING
    }
}
