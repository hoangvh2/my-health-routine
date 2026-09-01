package com.vh.health.core

import com.vh.health.core.program.KneeSignal
import com.vh.health.core.progress.BodyMetric
import com.vh.health.core.progress.KneeCheckIn
import com.vh.health.core.progress.SessionLog
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `ProgressRepository` (:app) stores these as JSON-encoded lists and cannot be
 * compiled or run in this container (see CLAUDE.md). This is the one place the exact
 * encode/decode round trip it relies on can actually be verified before it ships.
 */
class ProgressRecordsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `session logs round-trip through JSON`() {
        val logs = listOf(SessionLog(epochDay = 20_000, workoutId = "w_zone2_knee"), SessionLog(20_001, "w_strength_a"))
        val encoded = json.encodeToString(logs)
        assertEquals(logs, json.decodeFromString<List<SessionLog>>(encoded))
    }

    @Test
    fun `knee check-ins round-trip through JSON, signal included`() {
        val checkIns = listOf(
            KneeCheckIn(epochDay = 20_000, workoutId = "w_zone2_knee", signal = KneeSignal.LINGERING),
            KneeCheckIn(epochDay = 20_007, workoutId = "w_long_easy", signal = KneeSignal.OVERLOADED),
        )
        val encoded = json.encodeToString(checkIns)
        assertEquals(checkIns, json.decodeFromString<List<KneeCheckIn>>(encoded))
    }

    @Test
    fun `body metrics round-trip, including a partially-filled entry`() {
        val metrics = listOf(
            BodyMetric(epochDay = 20_000, weightKg = 78.4, waistCm = 92.0),
            BodyMetric(epochDay = 20_007, weightKg = 78.0, waistCm = null),
        )
        val encoded = json.encodeToString(metrics)
        assertEquals(metrics, json.decodeFromString<List<BodyMetric>>(encoded))
    }

    @Test
    fun `an empty list round-trips to an empty list, not an error`() {
        val encoded = json.encodeToString(emptyList<SessionLog>())
        assertEquals(emptyList(), json.decodeFromString<List<SessionLog>>(encoded))
    }
}
