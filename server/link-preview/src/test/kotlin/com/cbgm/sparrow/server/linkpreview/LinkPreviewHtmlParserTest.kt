package com.cbgm.sparrow.server.linkpreview

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkPreviewHtmlParserTest {
    @Test
    fun parsesOpenGraphMetadataAndResolvesRelativeImage() {
        val html =
            """
            <html>
              <head>
                <meta property="og:title" content="Sparrow &amp; Privacy">
                <meta property="og:description" content="Private messaging">
                <meta property="og:site_name" content="Example">
                <meta property="og:image" content="/image.png">
              </head>
            </html>
            """.trimIndent()

        val preview = parseLinkPreviewHtml("https://example.com/article", html)

        assertEquals("Sparrow & Privacy", preview.title)
        assertEquals("Private messaging", preview.description)
        assertEquals("Example", preview.siteName)
        assertEquals("https://example.com/image.png", preview.imageUrl)
    }

    @Test
    fun fallsBackToYouTubeWatchThumbnail() {
        val preview = parseLinkPreviewHtml(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "<html><head><title>Video</title></head></html>"
        )

        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", preview.imageUrl)
    }

    @Test
    fun resolvesYouTubeShortAndShortsThumbnailUrls() {
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            "https://youtu.be/dQw4w9WgXcQ?t=12".youtubeThumbnailUrlOrNull()
        )
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            "https://youtube.com/shorts/dQw4w9WgXcQ".youtubeThumbnailUrlOrNull()
        )
    }
}
