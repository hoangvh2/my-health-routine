package com.vh.health.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vh.health.core.schedule.DayTemplates
import com.vh.health.core.schedule.SleepLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "vh_health_settings")

/**
 * Persists the anchors. Times are stored as minutes past midnight so there is exactly
 * one representation on disk and no formatting to get wrong.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val WAKE = intPreferencesKey("wake_minutes")
        val BEDTIME = intPreferencesKey("bedtime_minutes")
        val SLEEP_TARGET = intPreferencesKey("sleep_target_minutes")
        val BEDTIME_FOLLOWS_WAKE = booleanPreferencesKey("bedtime_follows_wake")
        val MAIN_SESSION = intPreferencesKey("main_session_minutes")
        val TODAY_OVERRIDE = intPreferencesKey("today_start_override_minutes")
        val TODAY_WINDOW = intPreferencesKey("today_window_minutes")
        val OVERRIDE_DAY = intPreferencesKey("today_override_epoch_day")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        val overrideStillForToday =
            prefs[Keys.OVERRIDE_DAY] == LocalDate.now().toEpochDay().toInt()

        AppSettings(
            wakeTime = prefs[Keys.WAKE]?.toLocalTime() ?: DayTemplates.DEFAULT_WAKE,
            bedtime = prefs[Keys.BEDTIME]?.toLocalTime() ?: DayTemplates.DEFAULT_BEDTIME,
            sleepTargetMinutes = prefs[Keys.SLEEP_TARGET] ?: DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES,
            bedtimeFollowsWake = prefs[Keys.BEDTIME_FOLLOWS_WAKE] ?: true,
            mainSessionMinutes = prefs[Keys.MAIN_SESSION] ?: 33,
            todayStartOverride = if (overrideStillForToday) prefs[Keys.TODAY_OVERRIDE]?.toLocalTime() else null,
            todayWindowMinutes = if (overrideStillForToday) prefs[Keys.TODAY_WINDOW] else null,
        )
    }

    suspend fun setWakeTime(time: LocalTime) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.WAKE] = time.toMinutes()
            val follows = prefs[Keys.BEDTIME_FOLLOWS_WAKE] ?: true
            if (follows) {
                val target = prefs[Keys.SLEEP_TARGET] ?: DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES
                prefs[Keys.BEDTIME] = SleepLink.bedtimeFor(time, target).toMinutes()
            }
        }
    }

    suspend fun setBedtime(time: LocalTime) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.BEDTIME] = time.toMinutes()
            // Setting a bedtime by hand is the user taking manual control of it.
            prefs[Keys.BEDTIME_FOLLOWS_WAKE] = false
        }
    }

    suspend fun setBedtimeFollowsWake(follows: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.BEDTIME_FOLLOWS_WAKE] = follows
            if (follows) {
                val wake = prefs[Keys.WAKE]?.toLocalTime() ?: DayTemplates.DEFAULT_WAKE
                val target = prefs[Keys.SLEEP_TARGET] ?: DayTemplates.DEFAULT_SLEEP_TARGET_MINUTES
                prefs[Keys.BEDTIME] = SleepLink.bedtimeFor(wake, target).toMinutes()
            }
        }
    }

    suspend fun setMainSessionMinutes(minutes: Int) {
        context.settingsStore.edit { it[Keys.MAIN_SESSION] = minutes.coerceIn(12, 60) }
    }

    /** A start time that applies to today only. */
    suspend fun setTodayStart(time: LocalTime?) {
        context.settingsStore.edit { prefs ->
            if (time == null) {
                prefs.remove(Keys.TODAY_OVERRIDE)
            } else {
                prefs[Keys.TODAY_OVERRIDE] = time.toMinutes()
                prefs[Keys.OVERRIDE_DAY] = LocalDate.now().toEpochDay().toInt()
            }
        }
    }

    /** "Sáng nay chỉ có N phút". Pass null to go back to the full morning. */
    suspend fun setTodayWindow(minutes: Int?) {
        context.settingsStore.edit { prefs ->
            if (minutes == null) {
                prefs.remove(Keys.TODAY_WINDOW)
            } else {
                prefs[Keys.TODAY_WINDOW] = minutes
                prefs[Keys.OVERRIDE_DAY] = LocalDate.now().toEpochDay().toInt()
            }
        }
    }

    private fun Int.toLocalTime(): LocalTime = LocalTime.ofSecondOfDay((this % (24 * 60)) * 60L)

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute
}
