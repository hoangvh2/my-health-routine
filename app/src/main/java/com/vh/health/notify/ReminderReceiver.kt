package com.vh.health.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vh.health.MainActivity
import com.vh.health.R
import com.vh.health.VhHealthApp
import com.vh.health.core.notify.NotificationCopy
import com.vh.health.core.notify.ReminderContent
import com.vh.health.core.schedule.ReminderKind
import com.vh.health.core.schedule.ReminderSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires once per reminder per day. Two jobs, in this order: reschedule tomorrow's
 * occurrence (the daily cadence must survive even if posting fails), then post the
 * notification itself with whatever is true right now — today's actual workout,
 * the anchors as currently set — never anything baked in when the alarm was armed.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = ReminderKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_REMINDER_KIND) }
            ?: return
        val appContext = context.applicationContext
        // onReceive must return quickly and the process may be killed shortly after
        // it does; goAsync() buys enough time to finish the DataStore read below.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(appContext, kind)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, kind: ReminderKind) {
        val container = (context as VhHealthApp).container
        val settings = container.settings.settings.first()

        val time = ReminderSchedule.times(settings.wakeTime, settings.bedtime)
            .firstOrNull { it.kind == kind }?.time ?: return
        ReminderScheduler.scheduleOne(context, kind, time)

        val copy = when (kind) {
            ReminderKind.MORNING_START -> {
                val weekday = container.content.weekdayToday()
                ReminderContent.forMorningStart(time, container.content.program.workoutFor(weekday))
            }
            ReminderKind.EVENING_WIND_DOWN -> ReminderContent.forEveningWindDown(time, settings.bedtime)
            ReminderKind.DESK_BREAK_1 -> ReminderContent.forDeskBreak(0, time)
            ReminderKind.DESK_BREAK_2 -> ReminderContent.forDeskBreak(1, time)
            ReminderKind.DESK_BREAK_3 -> ReminderContent.forDeskBreak(2, time)
        }

        post(context, kind, copy)
    }

    private fun post(context: Context, kind: ReminderKind, copy: NotificationCopy) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val openAppIntent = PendingIntent.getActivity(
            context,
            kind.requestCode,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.channelFor(kind))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(copy.title)
            .setContentText(copy.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.bigText ?: copy.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        // notify() can throw SecurityException on some OEM builds even after the
        // areNotificationsEnabled() check above; never let a notification failure
        // crash the receiver and take the next day's reschedule down with it.
        runCatching { manager.notify(kind.requestCode, notification) }
    }
}
