package com.cbgm.sparrow.feature.ocr.data.parser

import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiptTextParserTest {
    @Test
    fun findsTotalAboveItsLabelBySummingTheItems() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    text(0, "Fleischerei und Partyservice"),
                    text(1, "Behrens GmbH & Co. KG", height = 40f),
                    text(2, "Bahnhofstraße 52"),
                    text(3, "18.04.2024 10:08:12"),
                    text(4, "Tatar"),
                    item(5, "0,152", "2,09", "3,18"),
                    text(6, "Twuster Mett im Glas"),
                    item(7, "5007", null, "3,30"),
                    item(8, "0,114", "3,59", "4,09"),
                    item(9, "0,132", "2,49", "3,29"),
                    item(10, "0,130", "5,49", "7,14"),
                    item(11, "0,152", "1,99", "3,02"),
                    amount(12, "24,02", height = 50f),
                    text(13, "6 Pos. Summe €")
                )
            )

        assertEquals(2_402L, scan.totalMinor)
        assertEquals("Behrens GmbH & Co. KG", scan.merchant)
    }

    @Test
    fun ignoresQuantityLinesAndPaymentRowsBelowTheTotal() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    text(0, "Quittung", height = 40f),
                    text(1, "Pager 33"),
                    centered(2, "A (1 x 1,60)"),
                    item(3, "Coca Cola 0,3l Menü", null, "1,60"),
                    centered(4, "A (1 x 2,30)"),
                    item(5, "Coca Cola 0,5l Menü", null, "2,30"),
                    centered(6, "A (1 x 2,30)"),
                    item(7, "Lift Apfelschorle 0,5l Menü", null, "2,30"),
                    centered(8, "A (3 x 6,80)"),
                    item(9, "Menü Pizza Salami", null, "20,40"),
                    item(10, "Total: EUR", null, "26,60"),
                    item(11, "Gegeben: EUR", null, "50,10"),
                    item(12, "Rückgeld: EUR", null, "23,50")
                )
            )

        assertEquals(2_660L, scan.totalMinor)
    }

    @Test
    fun ignoresCashAndChangeAfterTheTotal() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    text(0, "RECEIPT", height = 40f),
                    item(1, "1x Lorem ipsum", "$", "35.00"),
                    item(2, "2x Lorem ipsum", "$", "15.00"),
                    item(3, "1x Lorem ipsum", "$", "30.00"),
                    item(4, "1x Lorem ipsum", "$", "10.00"),
                    item(5, "2x Lorem ipsum", "$", "10.00"),
                    item(6, "1x Lorem ipsum", "$", "7.00"),
                    item(7, "1x Lorem ipsum", "$", "10.00"),
                    item(8, "TOTAL AMOUNT", null, "$ 117.00"),
                    item(9, "CASH", null, "$ 200.00"),
                    item(10, "CHANGE", null, "$ 83.00")
                )
            )

        assertEquals(11_700L, scan.totalMinor)
    }

    @Test
    fun usesTheOnlyAmountWhenThereAreNoItems() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    text(0, "Company Name"),
                    text(1, "PARKING RECEIPT", height = 30f),
                    text(2, "From: 10/01/2022 12:10"),
                    text(3, "To: 10/01/2022 13:40"),
                    centered(4, "PAID: 4.50")
                )
            )

        assertEquals(450L, scan.totalMinor)
        assertEquals("PARKING RECEIPT", scan.merchant)
    }

    @Test
    fun readsButcherReceiptWhoseTotalLostItsComma() {
        val scan = ReceiptTextParser.parse(BUTCHER_RECEIPT)

        assertEquals(2_402L, scan.totalMinor)
        assertEquals("Behrens GmbH & Co. KG", scan.merchant)
    }

    @Test
    fun readsAmountsWithCommonOcrMisreads() {
        val scan =
            ReceiptTextParser.parse(
                receipt(
                    item(0, "Tatar", null, "3,18"),
                    item(1, "Mett", null, "3,30"),
                    amount(2, "24,O2", height = 50f),
                    amount(3, "6, 48", height = 20f)
                )
            )

        assertEquals(2_402L, scan.totalMinor)
    }

    @Test
    fun returnsNothingForUnreadableReceipt() {
        val scan = ReceiptTextParser.parse(receipt(text(0, "12"), text(1, "##")))

        assertNull(scan.totalMinor)
        assertNull(scan.merchant)
    }

    private fun receipt(vararg rows: List<RecognizedTextLine>): List<RecognizedTextLine> =
        rows.flatMap { it }

    private fun text(index: Int, value: String, height: Float = 20f): List<RecognizedTextLine> =
        listOf(line(value, left = 0f, right = 300f, index = index, height = height))

    private fun centered(index: Int, value: String): List<RecognizedTextLine> =
        listOf(line(value, left = 200f, right = 400f, index = index))

    private fun amount(index: Int, value: String, height: Float): List<RecognizedTextLine> =
        listOf(line(value, left = 480f, right = 600f, index = index, height = height))

    private fun item(
        index: Int,
        label: String,
        middle: String?,
        value: String
    ): List<RecognizedTextLine> =
        listOfNotNull(
            line(label, left = 0f, right = 280f, index = index),
            middle?.let { line(it, left = 380f, right = 440f, index = index) },
            line(value, left = 520f, right = 600f, index = index)
        )

    private fun line(
        value: String,
        left: Float,
        right: Float,
        index: Int,
        height: Float = 20f
    ): RecognizedTextLine {
        val top = index * ROW_HEIGHT
        return RecognizedTextLine(value, left, top, right, bottom = top + height)
    }

    private companion object {
        const val ROW_HEIGHT = 60f

        val BUTCHER_RECEIPT =
            listOf(
                ocr("Fleischerei und Partyservice", 406, 328, 984, 382),
                ocr("Behrens GmbH & Co. KG", 342, 386, 1048, 470),
                ocr("Tatar", 296, 818, 404, 868),
                ocr("Abt. 1", 298, 664, 410, 700),
                ocr("18.04.202% 10:08:12", 300, 701, 572, 743),
                ocr("ka", 418, 766, 466, 810),
                ocr("Bahnhofstraße 52", 516, 488, 872, 538),
                ocr("THistringen", 582, 552, 810, 608),
                ocr("Tel. 04243/1838", 568, 604, 822, 652),
                ocr("0,152", 360, 880, 456, 937),
                ocr("THuster Mett im Glas", 296, 937, 751, 999),
                ocr("Artikel", 576, 758, 691, 803),
                ocr("Waage &", 674, 662, 788, 702),
                ocr("184/1", 674, 698, 750, 738),
                ocr("Sülze H", 293, 1479, 438, 1534),
                ocr("Trüffelleberwurst S", 296, 1069, 698, 1131),
                ocr("0,152", 357, 1547, 457, 1607),
                ocr("1503", 624, 894, 684, 927),
                ocr("0,114 4508", 360, 1146, 685, 1202),
                ocr("Zungen-Rotuurst H", 292, 1200, 687, 1272),
                ocr("6007", 620, 1024, 680, 1058),
                ocr("0,132 4604", 358, 1278, 684, 1336),
                ocr("Serano Schinken S", 292, 1337, 667, 1406),
                ocr("0,130 4132", 357, 1409, 683, 1474),
                ocr("18.04.2024 10:12:50", 288, 1810, 575, 1850),
                ocr("6Pos. Summe", 362, 1729, 648, 1787),
                ocr("4601", 623, 1561, 687, 1598),
                ocr("C/100g", 772, 758, 903, 803),
                ocr("€", 740, 1731, 773, 1779),
                ocr("2,09", 804, 873, 878, 930),
                ocr("3,59", 806, 1138, 882, 1196),
                ocr("2,49", 808, 1270, 884, 1328),
                ocr("5,49", 808, 1404, 886, 1462),
                ocr("1,99", 812, 1538, 886, 1600),
                ocr("# 005888", 944, 655, 1092, 693),
                ocr("Uark. 2", 945, 697, 1094, 736),
                ocr("** Vielen Dank **", 418, 1854, 978, 1953),
                ocr("€", 1057, 758, 1093, 802),
                ocr("F Es bediente Sie: Frau Brackland", 424, 1970, 978, 2016),
                ocr("3,18", 1022, 871, 1096, 927),
                ocr("3,30", 1024, 1004, 1098, 1060),
                ocr("4,09", 1026, 1136, 1100, 1192),
                ocr("3,29", 1028, 1268, 1104, 1326),
                ocr("7.14", 1030, 1402, 1106, 1460),
                ocr("2402", 933, 1639, 1111, 1798),
                ocr("3,02", 1032, 1538, 1108, 1598)
            )

        fun ocr(text: String, vararg box: Int): RecognizedTextLine {
            val (left, top, right, bottom) = box.map(Int::toFloat)
            return RecognizedTextLine(text, left, top, right, bottom)
        }
    }
}
