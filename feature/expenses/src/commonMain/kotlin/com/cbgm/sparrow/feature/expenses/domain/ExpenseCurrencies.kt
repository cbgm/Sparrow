package com.cbgm.sparrow.feature.expenses.domain

object ExpenseCurrencies {
    fun fractionDigits(currencyCode: String): Int =
        when (currencyCode) {
            in NO_FRACTION -> 0
            in THREE_FRACTION -> 3
            else -> 2
        }

    fun symbol(currencyCode: String): String = SYMBOLS[currencyCode] ?: currencyCode

    fun isMoney(currencyCode: String): Boolean = currencyCode !in NOT_MONEY

    private val NO_FRACTION =
        setOf(
            "BIF",
            "CLP",
            "DJF",
            "GNF",
            "ISK",
            "JPY",
            "KMF",
            "KRW",
            "PYG",
            "RWF",
            "UGX",
            "VND",
            "VUV",
            "XAF",
            "XOF",
            "XPF"
        )
    private val THREE_FRACTION = setOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND")
    private val NOT_MONEY = setOf("XAG", "XAU", "XDR", "XPD", "XPT")
    private val SYMBOLS =
        mapOf(
            "EUR" to "€",
            "USD" to "$",
            "GBP" to "£",
            "JPY" to "¥",
            "CNY" to "¥",
            "INR" to "₹",
            "KRW" to "₩",
            "TRY" to "₺",
            "PLN" to "zł",
            "ILS" to "₪",
            "THB" to "฿",
            "UAH" to "₴",
            "PHP" to "₱",
            "VND" to "₫"
        )
}
