package com.cbgm.sparrow.feature.media.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrCodeImageLayoutTest {
    @Test
    fun wrappingPreservesLongTextAndExplicitBlankLines() {
        val layout = QrCodeImageLayout(720)
        val text = "Long recipient name ".repeat(20)
        val wrapped = layout.wrap(listOf(text)) { it.length * 20.0 }
        assertEquals(text, wrapped.joinToString(""))
        assertTrue(wrapped.all { it.length * 20.0 <= layout.textWidth })
        assertEquals(listOf("a", "", "b"), layout.wrap(listOf("a\n\nb")) { it.length * 20.0 })
        assertTrue(layout.height(wrapped.size) > layout.height(1))
    }

    @Test
    fun wrappingDoesNotSplitASupplementaryUnicodeCharacter() {
        val layout = QrCodeImageLayout(720)
        val lines = layout.wrap(listOf("A😀B")) { text -> text.count { !it.isLowSurrogate() } * 600.0 }
        assertEquals(listOf("A", "😀", "B"), lines)
    }

    @Test
    fun renderRequestDoesNotPrintItsContentOrCaption() {
        val request = QrCodeRenderRequest(content = "Secret payload", caption = listOf("Secret recipient"))
        assertFalse(request.toString().contains("Secret"))
    }
}
