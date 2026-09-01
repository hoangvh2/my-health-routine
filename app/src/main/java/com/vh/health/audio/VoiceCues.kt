package com.vh.health.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Vietnamese voice-over for the session player, via Android's built-in
 * TextToSpeech — no audio asset, and it can speak whatever text the content ever
 * needs without a translator or a recording booth.
 */
class VoiceCues(context: Context) {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                val result = engine.setLanguage(Locale("vi", "VN"))
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    /** Queues after whatever is already speaking, rather than cutting it off. */
    fun announce(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "vh_cue_${System.nanoTime()}")
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
