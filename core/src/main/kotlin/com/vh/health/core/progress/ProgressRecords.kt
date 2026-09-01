package com.vh.health.core.progress

import com.vh.health.core.program.KneeSignal
import kotlinx.serialization.Serializable

/**
 * The M6 progress records. Plain data, no Android import — `ProgressRepository`
 * (:app) is the only place that adds DataStore storage around these, the same split
 * `AppSettings`/`SettingsRepository` already use. Declared here rather than in :app
 * so `@Serializable` has a serializer generated for it in a module the serialization
 * compiler plugin is actually applied to (:core; see `core/build.gradle.kts`) — :app
 * only needs the kotlinx-serialization-json *runtime* to encode/decode instances of
 * these, not the plugin itself.
 */

/** One completed workout. [epochDay] rather than a date type, matching how
 *  `AppSettings` stores its own day-stamps: exactly one representation on disk. */
@Serializable
data class SessionLog(val epochDay: Long, val workoutId: String)

/** What the knees reported after a `tracksKneeSignal` workout — see `KneeLoadPolicy`. */
@Serializable
data class KneeCheckIn(val epochDay: Long, val workoutId: String, val signal: KneeSignal)

/** A weigh-in. Either field may be recorded alone — the user may only track one. */
@Serializable
data class BodyMetric(val epochDay: Long, val weightKg: Double? = null, val waistCm: Double? = null)
