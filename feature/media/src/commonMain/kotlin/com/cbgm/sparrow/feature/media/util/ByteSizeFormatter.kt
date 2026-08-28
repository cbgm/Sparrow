package com.cbgm.sparrow.feature.media.util

fun Long.toReadableByteSize(): String =
    when {
        this >= BYTES_PER_GIGABYTE -> "${formatOneDecimal(this, BYTES_PER_GIGABYTE)} GB"
        this >= BYTES_PER_MEGABYTE -> "${formatOneDecimal(this, BYTES_PER_MEGABYTE)} MB"
        this >= BYTES_PER_KILOBYTE -> "${formatOneDecimal(this, BYTES_PER_KILOBYTE)} KB"
        else -> "$this B"
    }

private fun formatOneDecimal(value: Long, unit: Long): String {
    val tenths = (value * 10L) / unit
    val whole = tenths / 10L
    val decimal = tenths % 10L
    return if (decimal == 0L) "$whole" else "$whole.$decimal"
}

private const val BYTES_PER_KILOBYTE = 1024L
private const val BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1024L
private const val BYTES_PER_GIGABYTE = BYTES_PER_MEGABYTE * 1024L
