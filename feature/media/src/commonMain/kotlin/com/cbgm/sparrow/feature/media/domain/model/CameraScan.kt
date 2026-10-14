package com.cbgm.sparrow.feature.media.domain.model

enum class DocumentCaptureMode {
    AUTOMATIC,
    MANUAL
}

sealed interface CameraScanMode {
    data object QrCode : CameraScanMode

    data class Document(
        val captureMode: DocumentCaptureMode = DocumentCaptureMode.AUTOMATIC,
        val maxImageDimension: Int? = null,
        val maxImageBytes: Int? = null
    ) : CameraScanMode {
        init {
            maxImageDimension?.let { require(it > 0) { "Maximum image dimension must be positive" } }
            maxImageBytes?.let { require(it > 0) { "Maximum image bytes must be positive" } }
        }
    }
}

sealed interface CameraScanResult {
    data class QrCode(
        val value: String
    ) : CameraScanResult

    data class Document(
        val media: CapturedMedia
    ) : CameraScanResult
}
