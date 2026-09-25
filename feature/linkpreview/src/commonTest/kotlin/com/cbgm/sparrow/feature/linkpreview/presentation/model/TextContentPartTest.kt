package com.cbgm.sparrow.feature.linkpreview.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TextContentPartTest {
    @Test
    fun preservesTextAndLinkOrder() {
        val parts =
            "Before https://example.com/a between https://example.org/b after"
                .toTextContentParts()

        assertEquals(
            listOf(
                TextContentPart.Text("Before "),
                TextContentPart.LinkPreview("https://example.com/a"),
                TextContentPart.Text(" between "),
                TextContentPart.LinkPreview("https://example.org/b"),
                TextContentPart.Text(" after")
            ),
            parts
        )
    }

    @Test
    fun trailingPunctuationStaysText() {
        val parts = "See https://example.com/test, please.".toTextContentParts()

        assertEquals(
            listOf(
                TextContentPart.Text("See "),
                TextContentPart.LinkPreview("https://example.com/test"),
                TextContentPart.Text(", please.")
            ),
            parts
        )
    }
}
