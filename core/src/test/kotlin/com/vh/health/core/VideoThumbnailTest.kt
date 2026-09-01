package com.vh.health.core

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.content.youtubeThumbnailUrl
import com.vh.health.core.content.youtubeVideoId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoThumbnailTest {

    @Test
    fun `extracts the id from a standard watch URL`() {
        assertEquals("xyNwxiuERXc", youtubeVideoId("https://www.youtube.com/watch?v=xyNwxiuERXc"))
    }

    @Test
    fun `extracts the id from a youtu-be short link`() {
        assertEquals("xyNwxiuERXc", youtubeVideoId("https://youtu.be/xyNwxiuERXc"))
    }

    @Test
    fun `extracts the id from a shorts URL`() {
        assertEquals("xyNwxiuERXc", youtubeVideoId("https://www.youtube.com/shorts/xyNwxiuERXc"))
    }

    @Test
    fun `stops at trailing query parameters after the id`() {
        assertEquals("xyNwxiuERXc", youtubeVideoId("https://www.youtube.com/watch?v=xyNwxiuERXc&t=30s"))
    }

    @Test
    fun `a non-YouTube URL has no id and no thumbnail`() {
        assertNull(youtubeVideoId("https://vimeo.com/12345"))
        assertNull(youtubeThumbnailUrl("https://vimeo.com/12345"))
    }

    @Test
    fun `thumbnail URL is built from the extracted id`() {
        assertEquals(
            "https://img.youtube.com/vi/xyNwxiuERXc/hqdefault.jpg",
            youtubeThumbnailUrl("https://www.youtube.com/watch?v=xyNwxiuERXc"),
        )
    }

    @Test
    fun `every bundled video URL actually yields a thumbnail`() {
        val library = ContentLoader.loadLibrary()
        val withVideo = library.exercises.mapNotNull { it.videoUrl }
        assertTrue(withVideo.isNotEmpty(), "no bundled exercise has a videoUrl to check")
        withVideo.forEach { url -> assertTrue(youtubeThumbnailUrl(url) != null, "$url did not yield a thumbnail") }
    }
}
