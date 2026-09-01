package com.vh.health.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.vh.health.core.schedule.ReminderKind
import com.vh.health.core.schedule.ReminderSchedule
import com.vh.health.core.schedule.nextOccurrence
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val EXTRA_REMINDER_KIND = "reminder_kind"

/**
 * Arms and disarms the five reminder alarms. Not a "báo thức" in the alarm-clock
 * sense — no ringtone, no full-screen UI, no `setAlarmClock()` — just a quiet, exact
 * trigger that posts a notification. Android has no "repeat daily, exactly" alarm
 * primitive any more; the correct modern pattern is one-shot exact alarms that
 * reschedule themselves for tomorrow when they fire, which is what
 * [com.vh.health.notify.ReminderReceiver] does on receipt.
 */
object ReminderScheduler {

    fun scheduleAll(context: Context, wakeTime: LocalTime, bedtime: LocalTime) {
        ReminderSchedule.times(wakeTime, bedtime).forEach { reminderTime ->
            scheduleOne(context, reminderTime.kind, reminderTime.time)
        }
    }

    fun scheduleOne(
        context: Context,
        kind: ReminderKind,
        time: LocalTime,
        from: LocalDateTime = LocalDateTime.now(),
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(time, from)
        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = pendingIntentFor(context, kind)

        if (hasExactAlarmPermission(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // No exact-alarm permission: still schedule, just with more slack under
            // Doze, rather than silently scheduling nothing at all.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderKind.entries.forEach { kind -> alarmManager.cancel(pendingIntentFor(context, kind)) }
    }

    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true // restriction only exists from API 31
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /** Sends the user to the one system settings screen that can grant this — there
     *  is no in-app runtime dialog for it, unlike POST_NOTIFICATIONS. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    private fun pendingIntentFor(context: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("com.vh.health.REMINDER_${kind.name}")
            .putExtra(EXTRA_REMINDER_KIND, kind.name)
        return PendingIntent.getBroadcast(
            context,
            kind.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
