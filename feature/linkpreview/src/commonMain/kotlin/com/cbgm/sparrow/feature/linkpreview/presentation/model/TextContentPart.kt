package com.cbgm.sparrow.feature.linkpreview.presentation.model

sealed interface TextContentPart {
    data class Text(
        val text: String
    ) : TextContentPart

    data class LinkPreview(
        val url: String
    ) : TextContentPart
}

fun String.toTextContentParts(): List<TextContentPart> {
    if (isEmpty()) return emptyList()

    val parts = mutableListOf<TextContentPart>()
    var cursor = 0

    URL_REGEX.findAll(this).forEach { match ->
        val rawUrl = match.value
        val url = rawUrl.trimEnd(*TRAILING_LINK_PUNCTUATION)
        if (url.isEmpty()) return@forEach

        val urlStart = match.range.first
        val urlEndExclusive = urlStart + url.length

        if (cursor < urlStart) {
            parts += TextContentPart.Text(substring(cursor, urlStart))
        }

        parts += TextContentPart.LinkPreview(url)
        cursor = urlEndExclusive
    }

    if (cursor < length) {
        parts += TextContentPart.Text(substring(cursor))
    }

    return parts.ifEmpty {
        listOf(TextContentPart.Text(this))
    }
}

private val URL_REGEX = Regex("https?://[^\\s<>{}\\[\\]]+")
private val TRAILING_LINK_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', '\'', '"')
