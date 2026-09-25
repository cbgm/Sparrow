package com.cbgm.sparrow.server.linkpreview

import java.net.URI
import java.net.URISyntaxException

internal fun parseLinkPreviewHtml(
    pageUrl: String,
    html: String
): FetchedLinkPreview {
    val metadata = parseMetadata(html)
    val title = metadata["og:title"] ?: metadata["twitter:title"] ?: parseTitle(html)
    val description = metadata["og:description"] ?: metadata["twitter:description"] ?: metadata["description"]
    val siteName = metadata["og:site_name"] ?: pageUrl.hostOrNull()
    val rawImage = metadata["og:image"] ?: metadata["twitter:image"]
    val imageUrl =
        rawImage
            ?.takeIf(String::isNotBlank)
            ?.let { image -> pageUrl.resolveUrlOrNull(image) }
            ?: pageUrl.youtubeThumbnailUrlOrNull()

    return FetchedLinkPreview(
        url = pageUrl,
        title = title.cleanMetadata(),
        description = description.cleanMetadata(),
        siteName = siteName.cleanMetadata(),
        imageUrl = imageUrl
    )
}

private fun parseMetadata(html: String): Map<String, String> =
    buildMap {
        META_TAG_REGEX.findAll(html).forEach { match ->
            val attributes = parseAttributes(match.value)
            val key =
                attributes["property"]
                    ?: attributes["name"]
                    ?: return@forEach
            val content = attributes["content"] ?: return@forEach
            putIfAbsent(key.lowercase(), decodeHtmlEntities(content))
        }
    }

private fun parseAttributes(tag: String): Map<String, String> =
    buildMap {
        ATTRIBUTE_REGEX.findAll(tag).forEach { match ->
            val name = match.groupValues[1].lowercase()
            val value =
                match.groupValues[3]
                    .ifBlank { match.groupValues[4] }
                    .ifBlank { match.groupValues[5] }
            put(name, value)
        }
    }

private fun parseTitle(html: String): String? =
    TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)?.let(::decodeHtmlEntities)

private fun String?.cleanMetadata(): String? =
    this
        ?.replace(WHITESPACE_REGEX, " ")
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.take(MAX_METADATA_LENGTH)

private fun decodeHtmlEntities(value: String): String =
    value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace(NUMERIC_ENTITY_REGEX) { match ->
            match.groupValues[1]
                .toIntOrNull()
                ?.takeIf { codePoint -> Character.isValidCodePoint(codePoint) }
                ?.let { codePoint -> String(Character.toChars(codePoint)) }
                ?: match.value
        }

internal fun String.youtubeThumbnailUrlOrNull(): String? {
    return try {
        val uri = URI(this)
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val videoId =
            when {
                host == "youtu.be" -> uri.path.trim('/').substringBefore('/').takeIf(String::isNotBlank)
                host == "youtube.com" || host == "m.youtube.com" ->
                    when {
                        uri.path == "/watch" -> uri.rawQuery.queryParameter("v")
                        uri.path.startsWith("/shorts/") -> uri.path.removePrefix("/shorts/").substringBefore('/')
                        uri.path.startsWith("/embed/") -> uri.path.removePrefix("/embed/").substringBefore('/')
                        else -> null
                    }
                else -> null
            }
        videoId
            ?.takeIf(YOUTUBE_VIDEO_ID_REGEX::matches)
            ?.let { id -> "https://i.ytimg.com/vi/$id/hqdefault.jpg" }
    } catch (_: URISyntaxException) {
        null
    }
}

private fun String?.queryParameter(name: String): String? =
    this
        ?.split('&')
        ?.firstOrNull { parameter -> parameter.substringBefore('=') == name }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.takeIf(String::isNotBlank)

private fun String.hostOrNull(): String? =
    try {
        URI(this).host
    } catch (_: URISyntaxException) {
        null
    }

private fun String.resolveUrlOrNull(relativeUrl: String): String? =
    try {
        URI(this).resolve(relativeUrl).toString()
    } catch (_: URISyntaxException) {
        null
    }

private val META_TAG_REGEX = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ATTRIBUTE_REGEX = Regex("([A-Za-z_:.-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))")
private val TITLE_REGEX = Regex("<title\\b[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val NUMERIC_ENTITY_REGEX = Regex("&#(\\d+);")
private val WHITESPACE_REGEX = Regex("\\s+")
private val YOUTUBE_VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{6,}")
private const val MAX_METADATA_LENGTH = 2_000
