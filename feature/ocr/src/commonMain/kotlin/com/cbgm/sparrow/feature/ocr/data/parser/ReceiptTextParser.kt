package com.cbgm.sparrow.feature.ocr.data.parser

import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan
import kotlin.math.abs

internal object ReceiptTextParser {
    fun parse(lines: List<RecognizedTextLine>): ReceiptScan {
        val rows = lines.filter { it.text.isNotBlank() }.toRows()
        return ReceiptScan(
            totalMinor = findTotal(rows),
            merchant = findMerchant(rows)
        )
    }

    private fun List<RecognizedTextLine>.toRows(): List<String> {
        val rows = mutableListOf<MutableList<RecognizedTextLine>>()
        sortedBy { it.centerY }.forEach { line ->
            val row = rows.lastOrNull()
            if (row != null && row.sharesRowWith(line)) row += line else rows += mutableListOf(line)
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }

    private fun List<RecognizedTextLine>.sharesRowWith(line: RecognizedTextLine): Boolean {
        val rowCenter = sumOf { it.centerY.toDouble() } / size
        val tolerance = maxOf(line.height, maxOf { it.height }) * ROW_TOLERANCE
        return abs(line.centerY - rowCenter) <= tolerance
    }

    private fun findTotal(rows: List<String>): Long? {
        val labelled =
            rows.indices.mapNotNull { index ->
                val row = rows[index].normalized()
                if (!row.containsAny(TOTAL_KEYWORDS) || row.containsAny(EXCLUDED_KEYWORDS)) {
                    return@mapNotNull null
                }
                rows[index].amounts().lastOrNull()
                    ?: rows.getOrNull(index + 1)?.amounts()?.lastOrNull()
            }
        if (labelled.isNotEmpty()) return labelled.max()
        return rows
            .filterNot { it.normalized().containsAny(EXCLUDED_KEYWORDS) }
            .flatMap { it.amounts() }
            .maxOrNull()
    }

    private fun findMerchant(rows: List<String>): String? =
        rows.take(MERCHANT_SEARCH_ROWS)
            .map { it.replace(WHITESPACE, " ").trim().trimEnd('.', ',', ':', ';', '-') }
            .firstOrNull { row -> row.looksLikeMerchant() }
            ?.take(MAX_MERCHANT_LENGTH)

    private fun String.looksLikeMerchant(): Boolean {
        val letters = count { it.isLetter() }
        val visible = count { !it.isWhitespace() }
        return letters >= MIN_MERCHANT_LETTERS &&
            letters >= visible * MIN_MERCHANT_LETTER_SHARE &&
            amounts().isEmpty() &&
            '@' !in this &&
            !normalized().containsAny(MERCHANT_EXCLUDED_KEYWORDS)
    }

    private fun String.amounts(): List<Long> =
        AMOUNT.findAll(this)
            .filterNot { match -> match.range.first > 0 && this[match.range.first - 1] == '-' }
            .mapNotNull { match ->
                val major = match.groupValues[1].filter(Char::isDigit).toLongOrNull()
                val minor = match.groupValues[2].toLongOrNull()
                if (major == null || minor == null) null else major * 100 + minor
            }.filter { it in 1..MAX_TOTAL_MINOR }
            .toList()

    private fun String.normalized(): String = lowercase().replace(WHITESPACE, " ")

    private fun String.containsAny(keywords: Regex): Boolean = keywords.containsMatchIn(this)

    private fun keywords(vararg words: String): Regex =
        Regex("(?<!\\p{L})(?:${words.joinToString("|") { Regex.escape(it) }})(?!\\p{L})")

    private val RecognizedTextLine.centerY: Float get() = (top + bottom) / 2f

    private val RecognizedTextLine.height: Float get() = bottom - top

    private val AMOUNT =
        Regex("(?<![\\d.,])(\\d{1,3}(?:[ .,']\\d{3})+|\\d+)[.,](\\d{2})(?![.,]?\\d)")
    private val WHITESPACE = Regex("\\s+")

    private val TOTAL_KEYWORDS =
        keywords(
            "summe",
            "gesamt",
            "gesamtbetrag",
            "gesamtsumme",
            "endsumme",
            "endbetrag",
            "zu zahlen",
            "zahlbetrag",
            "total",
            "grand total",
            "amount due",
            "balance due",
            "to pay"
        )
    private val EXCLUDED_KEYWORDS =
        keywords(
            "zwischensumme",
            "subtotal",
            "sub total",
            "netto",
            "mwst",
            "ust",
            "steuer",
            "tax",
            "vat",
            "gegeben",
            "rückgeld",
            "rueckgeld",
            "wechselgeld",
            "change",
            "bar",
            "cash",
            "tendered",
            "trinkgeld",
            "tip"
        )
    private val MERCHANT_EXCLUDED_KEYWORDS =
        keywords(
            "kassenbon",
            "beleg",
            "rechnung",
            "quittung",
            "receipt",
            "invoice",
            "willkommen",
            "welcome",
            "tel",
            "telefon",
            "fax",
            "www",
            "http",
            "datum",
            "date",
            "uhr",
            "filiale",
            "kasse"
        )

    private const val ROW_TOLERANCE = 0.5f
    private const val MERCHANT_SEARCH_ROWS = 6
    private const val MIN_MERCHANT_LETTERS = 3
    private const val MIN_MERCHANT_LETTER_SHARE = 0.6f
    private const val MAX_MERCHANT_LENGTH = 60
    private const val MAX_TOTAL_MINOR = 100_000_000L
}
