package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

private const val LINK_REGEX = "(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?)"

@Composable
fun rememberLinkAnnotatedString(
    text: String,
    linkColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
): AnnotatedString = remember(text, linkColor) {
    buildAnnotatedString {
        val urlRegex = LINK_REGEX.toRegex()
        var lastMatchEnd = 0

        urlRegex.findAll(text).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1

            append(text.substring(lastMatchEnd, startIndex))

            val url = matchResult.value
            val linkAnnotation = LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
            )

            withLink(linkAnnotation) {
                append(url)
            }

            lastMatchEnd = endIndex
        }

        if (lastMatchEnd < text.length) {
            append(text.substring(lastMatchEnd))
        }
    }
}
