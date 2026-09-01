package com.vh.health.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Requests transient "duck" audio focus once, for the whole workout session — never
 * per beep. Ducking the user's own music down and back up for every countdown tick
 * would be far more jarring than one polite dip that holds for the session and
 * releases when it ends, per docs/DECISIONS.md (D-008).
 */
class PlaybackFocus(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null

    fun requestDucking() {
        if (request != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { /* nothing to react to: we never play continuously enough to be told to stop */ }
            .build()
        audioManager.requestAudioFocus(focusRequest)
        request = focusRequest
    }

    fun release() {
        request?.let { audioManager.abandonAudioFocusRequest(it) }
        request = null
    }
}
