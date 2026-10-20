package com.cbgm.sparrow.feature.ocr.data.parser

import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan
import kotlin.math.abs

internal object ReceiptTextParser {
    fun parse(lines: List<RecognizedTextLine>): ReceiptScan {
        val visible = lines.filter { it.text.isNotBlank() }
        val rows = visible.toRows()
        val width =
            (visible.maxOfOrNull { it.right } ?: 0f) - (visible.minOfOrNull { it.left } ?: 0f)
        return ReceiptScan(
            totalMinor = findTotal(rows, width),
            merchant = findMerchant(rows)
        )
    }

    private fun List<RecognizedTextLine>.toRows(): List<List<RecognizedTextLine>> {
        val rows = mutableListOf<MutableList<RecognizedTextLine>>()
        sortedBy { it.centerY }.forEach { line ->
            val row = rows.lastOrNull()
            if (row != null && row.sharesRowWith(line)) row += line else rows += mutableListOf(line)
        }
        return rows.map { row -> row.sortedBy { it.left } }
    }

    private fun List<RecognizedTextLine>.sharesRowWith(line: RecognizedTextLine): Boolean {
        val rowCenter = sumOf { it.centerY.toDouble() } / size
        val tolerance = maxOf(line.height, maxOf { it.height }) * ROW_TOLERANCE
        return abs(line.centerY - rowCenter) <= tolerance
    }

    private fun findTotal(rows: List<List<RecognizedTextLine>>, width: Float): Long? {
        val cells = rows.mapNotNull { row -> row.amountCell() }
        val alignment = width * ALIGNMENT_TOLERANCE
        cells.forEachIndexed { index, total ->
            if (total.isSumOf(cells.subList(0, index).asReversed(), alignment)) return total.value
        }
        val tallest = cells.maxOfOrNull { it.height } ?: return null
        return cells
            .filter { it.height >= tallest * TALLEST_SHARE }
            .groupingBy { it.value }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Long, Int>> { it.value }.thenBy { it.key })
            ?.key
    }

    private fun AmountCell.isSumOf(above: List<AmountCell>, alignment: Float): Boolean {
        var sum = 0L
        var count = 0
        for (item in above) {
            if (!item.exact || abs(item.right - right) > alignment) continue
            sum += item.value
            count++
            if (sum == value && count >= MIN_SUMMED_ITEMS) return true
            if (sum > value) return false
        }
        return false
    }

    private fun List<RecognizedTextLine>.amountCell(): AmountCell? =
        asReversed().firstNotNullOfOrNull { line ->
            val exact = line.text.amounts().lastOrNull()
            (exact ?: line.text.amountWithoutSeparator())?.let { value ->
                AmountCell(value, line.right, line.height, exact = exact != null)
            }
        }

    private fun String.amountWithoutSeparator(): Long? =
        filterNot { it.isWhitespace() || it in CURRENCY_SYMBOLS }
            .takeIf { digits -> digits.length in BARE_AMOUNT_DIGITS && digits.all(Char::isDigit) }
            ?.toLongOrNull()

    private fun findMerchant(rows: List<List<RecognizedTextLine>>): String? =
        rows.takeWhile { row -> row.none { it.text.amounts().isNotEmpty() } }
            .map { row -> row.text() to row.maxOf { it.height } }
            .filter { (text, _) -> text.looksLikeName() }
            .maxByOrNull { (_, height) -> height }
            ?.first
            ?.take(MAX_MERCHANT_LENGTH)

    private fun List<RecognizedTextLine>.text(): String =
        joinToString(" ") { it.text.trim() }
            .replace(WHITESPACE, " ")
            .trimEnd('.', ',', ':', ';', '-')

    private fun String.looksLikeName(): Boolean {
        val letters = count { it.isLetter() }
        val visible = count { !it.isWhitespace() }
        return letters >= MIN_MERCHANT_LETTERS && letters >= visible * MIN_MERCHANT_LETTER_SHARE
    }

    private fun String.amounts(): List<Long> = withOcrDigitFixes().amountsIn()

    private fun String.withOcrDigitFixes(): String =
        replace(LETTER_O_NEXT_TO_DIGIT, "0").replace(LOW_QUOTE_COMMA, ",")

    private fun String.amountsIn(): List<Long> =
        AMOUNT.findAll(this)
            .filterNot { match -> match.range.first > 0 && this[match.range.first - 1] == '-' }
            .mapNotNull { match ->
                val major = match.groupValues[1].filter(Char::isDigit).toLongOrNull()
                val minor = match.groupValues[2].toLongOrNull()
                if (major == null || minor == null) null else major * 100 + minor
            }.filter { it in 1..MAX_TOTAL_MINOR }
            .toList()

    private val RecognizedTextLine.centerY: Float get() = (top + bottom) / 2f

    private val RecognizedTextLine.height: Float get() = bottom - top

    private class AmountCell(
        val value: Long,
        val right: Float,
        val height: Float,
        val exact: Boolean
    )

    private val AMOUNT =
        Regex("(?<![\\d.,])(\\d{1,3}(?:[ .,']\\d{3})+|\\d+)[.,] ?(\\d{2})(?![.,]?\\d)")
    private val WHITESPACE = Regex("\\s+")
    private val LETTER_O_NEXT_TO_DIGIT = Regex("(?<=[\\d.,])[Oo]|[Oo](?=[\\d.,]\\d)")
    private val LOW_QUOTE_COMMA = Regex("[\u201A\u060C]")

    private const val CURRENCY_SYMBOLS = "€$£¥"
    private val BARE_AMOUNT_DIGITS = 3..7

    private const val ROW_TOLERANCE = 0.5f
    private const val ALIGNMENT_TOLERANCE = 0.04f
    private const val TALLEST_SHARE = 0.9f
    private const val MIN_SUMMED_ITEMS = 2
    private const val MIN_MERCHANT_LETTERS = 3
    private const val MIN_MERCHANT_LETTER_SHARE = 0.6f
    private const val MAX_MERCHANT_LENGTH = 60
    private const val MAX_TOTAL_MINOR = 100_000_000L
}
