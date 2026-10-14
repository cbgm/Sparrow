package com.cbgm.sparrow.feature.ocr.data.parser

import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiptTextParserTest {
    @Test
    fun readsTotalFromLabelAndAmountInSeparateColumns() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    row(0, "REWE Markt GmbH"),
                    row(1, "Musterstr. 12, 10115 Berlin"),
                    row(3, "Bananen", "1,99"),
                    row(4, "Milch 1,5%", "1,29"),
                    row(5, "Zwischensumme", "3,28"),
                    row(6, "SUMME EUR", "13,28"),
                    row(7, "Gegeben Bar", "20,00"),
                    row(8, "Rückgeld", "6,72"),
                    row(9, "MwSt 7%", "0,87")
                )
            )

        assertEquals(1_328L, scan.totalMinor)
        assertEquals("REWE Markt GmbH", scan.merchant)
    }

    @Test
    fun readsAmountFromRowBelowLabel() {
        val scan =
            ReceiptTextParser.parse(
                receipt(row(0, "Cafe Blau"), row(1, "Total"), row(2, "EUR 1.234,50"))
            )

        assertEquals(123_450L, scan.totalMinor)
    }

    @Test
    fun fallsBackToLargestAmountWithoutTotalLabel() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    row(0, "Kiosk"),
                    row(1, "Kaffee", "2.50"),
                    row(2, "Kuchen", "3.80"),
                    row(3, "Cash", "10.00")
                )
            )

        assertEquals(380L, scan.totalMinor)
    }

    @Test
    fun ignoresDatesTimesAndNegativeAmounts() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    row(0, "Kassenbon"),
                    row(1, "Bäckerei Müller"),
                    row(2, "14.10.26 12:31"),
                    row(3, "Rabatt", "-1,00"),
                    row(4, "Gesamt", "4,20")
                )
            )

        assertEquals(420L, scan.totalMinor)
        assertEquals("Bäckerei Müller", scan.merchant)
    }

    @Test
    fun returnsNothingForUnreadableReceipt() {
        val scan = ReceiptTextParser.parse(receipt(row(0, "12"), row(1, "##")))

        assertNull(scan.totalMinor)
        assertNull(scan.merchant)
    }

    private fun receipt(vararg rows: List<RecognizedTextLine>): List<RecognizedTextLine> =
        rows.flatMap { it }

    private fun row(index: Int, label: String, amount: String? = null): List<RecognizedTextLine> {
        val top = index * LINE_HEIGHT
        return listOfNotNull(
            RecognizedTextLine(label, left = 0f, top = top, right = 300f, bottom = top + 20f),
            amount?.let {
                RecognizedTextLine(it, left = 500f, top = top + 2f, right = 600f, bottom = top + 22)
            }
        )
    }

    private companion object {
        const val LINE_HEIGHT = 30f
    }
}
