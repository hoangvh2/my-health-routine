package com.vh.health.core.content

/**
 * Turns a curated [Exercise.videoUrl] into a still preview the player can show
 * inline, right under the countdown ring, instead of only a tap-out-to-browser
 * button. Nothing is downloaded or bundled — this builds a URL to YouTube's own
 * public thumbnail endpoint, loaded at runtime same as the video link itself, so it
 * stays inside the boundary in docs/DECISIONS.md (D-006, D-008). A URL that isn't
 * recognizably a YouTube link yields no id and no thumbnail — the player just shows
 * nothing for it rather than a broken image request.
 */
private val YOUTUBE_ID_PATTERN =
    Regex("""(?:youtube\.com/watch\?v=|youtube\.com/shorts/|youtu\.be/)([A-Za-z0-9_-]{11})""")

fun youtubeVideoId(url: String): String? = YOUTUBE_ID_PATTERN.find(url)?.groupValues?.get(1)

fun youtubeThumbnailUrl(url: String): String? =
    youtubeVideoId(url)?.let { id -> "https://img.youtube.com/vi/$id/hqdefault.jpg" }
