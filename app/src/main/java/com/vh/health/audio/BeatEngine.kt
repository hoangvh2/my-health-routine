package com.vh.health.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Synthesises the tabata beat and cue tones at runtime instead of bundling an audio
 * file — see docs/DECISIONS.md (D-006). Every sound here is generated PCM, so the
 * beat is always exactly in phase with [com.vh.health.core.session.SessionCursor];
 * a looped MP3 can never make that promise.
 */
class BeatEngine {
    private val sampleRate = 44_100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** A short pure tone, faded in and out over a few ms so it doesn't click. */
    private fun tone(freqHz: Double, durationMs: Int, volume: Float): ShortArray {
        val n = sampleRate * durationMs / 1000
        val fade = min(n / 4, sampleRate / 100) // ~10ms, capped so very short tones still fade
        return ShortArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < fade -> i.toFloat() / fade
                i > n - fade -> (n - i).toFloat() / fade
                else -> 1f
            }
            (sin(2.0 * PI * freqHz * t) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Concatenates one or more tones into a single buffer and plays it once, so a
     *  two-note phrase (see [finished]) plays in sequence rather than as a chord. */
    private fun play(vararg segments: ShortArray) {
        val combined = ShortArray(segments.sumOf { it.size })
        var offset = 0
        for (segment in segments) {
            segment.copyInto(combined, offset)
            offset += segment.size
        }
        scope.launch {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack(
                attrs, format, combined.size * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE,
            )
            track.write(combined, 0, combined.size)
            track.play()
            delay(combined.size * 1000L / sampleRate + 40)
            track.stop()
            track.release()
        }
    }

    /** A short tick during the last three seconds of a segment. */
    fun countInTick() = play(tone(1046.5, 90, 0.55f)) // C6

    /** The start of a WORK segment: bright and upward. */
    fun workStart() = play(tone(1318.5, 160, 0.6f)) // E6

    /** The start of a REST segment: lower and calmer. */
    fun restStart() = play(tone(659.3, 220, 0.5f)) // E5

    /** The whole session is done. */
    fun finished() = play(tone(1046.5, 140, 0.6f), tone(1318.5, 220, 0.6f))

    /** Stops the engine's coroutine scope; call from onCleared(). */
    fun release() = scope.cancel()
}
