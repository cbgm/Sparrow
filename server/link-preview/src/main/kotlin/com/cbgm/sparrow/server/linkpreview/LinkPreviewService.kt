package com.cbgm.sparrow.server.linkpreview

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal class LinkPreviewService(
    private val fetcher: LinkPreviewFetcher,
    private val cacheTtlMilliseconds: Long
) {
    private val previews = ConcurrentHashMap<String, CachedPreview>()
    private val imageSources = ConcurrentHashMap<String, CachedImageSource>()

    suspend fun getPreview(url: String): LinkPreviewResponse {
        val now = System.currentTimeMillis()
        previews[url]
            ?.takeIf { cached -> cached.expiresAtEpochMilliseconds > now }
            ?.let { cached -> return cached.preview }

        val fetched = fetcher.fetch(url)
        val imageId = fetched.imageUrl?.let(::imageId)
        val expiresAt = now + cacheTtlMilliseconds
        val preview =
            LinkPreviewResponse(
                url = fetched.url,
                title = fetched.title,
                description = fetched.description,
                siteName = fetched.siteName,
                imagePath = imageId?.let { id -> "/v1/link-preview/images/$id" }
            )

        previews[url] = CachedPreview(preview, expiresAt)
        if (imageId != null) {
            imageSources[imageId] =
                CachedImageSource(
                    sourceUrl = fetched.imageUrl,
                    expiresAtEpochMilliseconds = expiresAt
                )
        }

        cleanupExpired(now)
        return preview
    }

    suspend fun getImage(imageId: String): LinkPreviewImage? {
        val now = System.currentTimeMillis()
        val source =
            imageSources[imageId]
                ?.takeIf { cached -> cached.expiresAtEpochMilliseconds > now }
                ?: return null

        return fetcher.fetchImage(source.sourceUrl)
    }

    private fun cleanupExpired(now: Long) {
        previews.entries.removeIf { it.value.expiresAtEpochMilliseconds <= now }
        imageSources.entries.removeIf { it.value.expiresAtEpochMilliseconds <= now }
    }

    private fun imageId(url: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(url.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class CachedPreview(
        val preview: LinkPreviewResponse,
        val expiresAtEpochMilliseconds: Long
    )

    private data class CachedImageSource(
        val sourceUrl: String,
        val expiresAtEpochMilliseconds: Long
    )
}
