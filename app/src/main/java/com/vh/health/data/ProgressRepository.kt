package com.vh.health.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.progress.BodyMetric
import com.vh.health.core.progress.KneeCheckIn
import com.vh.health.core.progress.SessionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.progressStore: DataStore<Preferences> by preferencesDataStore(name = "vh_health_progress")

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> Preferences.decodeList(key: Preferences.Key<String>): List<T> {
    val raw = this[key] ?: return emptyList()
    // A record shape can change across app versions; a value that no longer parses
    // should read back as "nothing logged yet", never crash the Progress screen.
    return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
}

private inline fun <reified T> MutablePreferences.encodeList(key: Preferences.Key<String>, value: List<T>) {
    this[key] = json.encodeToString(value)
}

/**
 * M6 progress data: sessions completed, knee check-ins, and body metrics. The record
 * shapes ([SessionLog], [KneeCheckIn], [BodyMetric]) live in `:core` — see
 * `core/progress/ProgressRecords.kt` — and are tested there via a real encode/decode
 * round trip, since `:app` cannot be compiled or run in this container.
 *
 * Stored as JSON-encoded lists in DataStore rather than Room: everything here is a
 * handful of records a day, read back only as "recent N" or "all", so a real database
 * would buy nothing but first-time KSP/Room build risk in an environment that cannot
 * catch it locally. See docs/DECISIONS.md (D-009), which amends D-003's original
 * "Room arrives with M6".
 */
class ProgressRepository(private val context: Context) {

    private object Keys {
        val SESSIONS = stringPreferencesKey("session_logs")
        val KNEE_CHECK_INS = stringPreferencesKey("knee_check_ins")
        val BODY_METRICS = stringPreferencesKey("body_metrics")
    }

    val sessions: Flow<List<SessionLog>> = context.progressStore.data.map { it.decodeList(Keys.SESSIONS) }
    val kneeCheckIns: Flow<List<KneeCheckIn>> = context.progressStore.data.map { it.decodeList(Keys.KNEE_CHECK_INS) }
    val bodyMetrics: Flow<List<BodyMetric>> = context.progressStore.data.map { it.decodeList(Keys.BODY_METRICS) }

    /** Logs one finished session. Idempotent for the same day+workout, so re-entering
     *  an already-finished player screen (e.g. after a process death mid-review)
     *  cannot double-count a streak day. */
    suspend fun logSession(epochDay: Long, workoutId: String) {
        context.progressStore.edit { prefs ->
            val existing = prefs.decodeList<SessionLog>(Keys.SESSIONS)
            if (existing.any { it.epochDay == epochDay && it.workoutId == workoutId }) return@edit
            prefs.encodeList(Keys.SESSIONS, existing + SessionLog(epochDay, workoutId))
        }
    }

    /** One check-in per workout session: answering twice (e.g. backing out and back
     *  in) replaces the earlier answer rather than logging both. */
    suspend fun logKneeCheckIn(epochDay: Long, workoutId: String, signal: KneeSignal) {
        context.progressStore.edit { prefs ->
            val kept = prefs.decodeList<KneeCheckIn>(Keys.KNEE_CHECK_INS)
                .filterNot { it.epochDay == epochDay && it.workoutId == workoutId }
            prefs.encodeList(Keys.KNEE_CHECK_INS, kept + KneeCheckIn(epochDay, workoutId, signal))
        }
    }

    /** One entry per day: a second save on the same day corrects the first rather
     *  than adding a duplicate point. */
    suspend fun logBodyMetric(epochDay: Long, weightKg: Double?, waistCm: Double?) {
        context.progressStore.edit { prefs ->
            val kept = prefs.decodeList<BodyMetric>(Keys.BODY_METRICS).filterNot { it.epochDay == epochDay }
            val updated = (kept + BodyMetric(epochDay, weightKg, waistCm)).sortedBy { it.epochDay }
            prefs.encodeList(Keys.BODY_METRICS, updated)
        }
    }
}
