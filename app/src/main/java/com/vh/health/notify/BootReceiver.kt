package com.vh.health.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vh.health.VhHealthApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android clears every exact alarm on reboot. Without this, reminders would
 * silently stop the moment the phone restarts — indistinguishable from the app
 * simply having stopped working — until the user happened to open Settings again.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as VhHealthApp).container
                val settings = container.settings.settings.first()
                if (settings.remindersEnabled) {
                    ReminderScheduler.scheduleAll(appContext, settings.wakeTime, settings.bedtime)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
