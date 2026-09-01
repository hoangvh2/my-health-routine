package com.vh.health

import android.app.Application
import android.content.Context
import com.vh.health.data.ContentRepository
import com.vh.health.data.SettingsRepository

/**
 * Dependencies are wired by hand rather than by a DI framework: the graph is small,
 * and skipping annotation processing keeps the build fast and the failure modes few.
 * See docs/DECISIONS.md (D-004).
 */
class AppContainer(context: Context) {
    /** Kept for the few callers that need a raw Context — TextToSpeech, opening a link. */
    val appContext: Context = context.applicationContext
    val settings: SettingsRepository = SettingsRepository(appContext)
    val content: ContentRepository = ContentRepository()
}

class VhHealthApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
