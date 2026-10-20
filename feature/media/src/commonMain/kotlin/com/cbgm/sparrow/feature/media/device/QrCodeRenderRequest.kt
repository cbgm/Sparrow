package com.cbgm.sparrow.feature.media.device

enum class QrErrorCorrection { LOW, MEDIUM, QUARTILE, HIGH }

data class QrCodeRenderRequest(
    val content: String,
    val size: Int = 800,
    val margin: Int = 1,
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.LOW,
    val caption: List<String> = emptyList()
) {
    init {
        require(content.isNotEmpty()) { "QR content must not be empty" }
        require(size > 0 && margin in 0..size / 2) { "Invalid QR dimensions" }
    }

    override fun toString(): String =
        "QrCodeRenderRequest(size=$size, margin=$margin, errorCorrection=$errorCorrection)"
}
