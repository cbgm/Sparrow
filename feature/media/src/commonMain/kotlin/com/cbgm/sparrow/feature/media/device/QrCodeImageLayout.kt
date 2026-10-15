package com.cbgm.sparrow.feature.media.device

/** Shared layout for captioned images; all positions are pixels. */
internal class QrCodeImageLayout(
    qrSize: Int
) {
    val width = qrSize + QR_HORIZONTAL_PADDING * 2
    val qrLeft = QR_HORIZONTAL_PADDING
    val qrTop = QR_TOP_PADDING
    val textLeft = TEXT_PADDING
    val textTop = qrTop + qrSize + CAPTION_GAP
    val textWidth = width - TEXT_PADDING * 2
    val textSize = FONT_SIZE
    val lineHeight = LINE_HEIGHT

    fun height(lineCount: Int): Int = textTop + lineCount * lineHeight + TEXT_PADDING

    fun wrap(lines: List<String>, measure: (String) -> Double): List<String> = buildList {
        lines.forEach { line ->
            line.split('\n').forEach { paragraph ->
                if (paragraph.isEmpty()) add("")
                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    var end = remaining.length
                    while (end > 1 && measure(remaining.take(end)) > textWidth) end--
                    // Keep a UTF-16 surrogate pair together when a line wraps.
                    if (end < remaining.length && remaining[end - 1].isHighSurrogate() &&
                        remaining[end].isLowSurrogate()
                    ) {
                        end = if (end == 1) 2 else end - 1
                    }
                    add(remaining.take(end))
                    remaining = remaining.drop(end)
                }
            }
        }
    }

    private companion object {
        const val QR_HORIZONTAL_PADDING = 180
        const val QR_TOP_PADDING = 30
        const val TEXT_PADDING = 50
        const val CAPTION_GAP = 60
        const val FONT_SIZE = 34
        const val LINE_HEIGHT = 52
    }
}
