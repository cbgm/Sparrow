package com.cbgm.sparrow.feature.ocr.data.model

data class RecognizedTextLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
