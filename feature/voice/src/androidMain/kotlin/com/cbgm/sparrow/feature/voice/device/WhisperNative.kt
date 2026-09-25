package com.cbgm.sparrow.feature.voice.device

internal fun interface WhisperProgressCallback {
    @Suppress("unused")
    fun onProgress(progressPercent: Int)
}

internal class WhisperNative {
    external fun loadModel(modelPath: String): Long

    external fun transcribe(
        modelHandle: Long,
        samples: FloatArray,
        progressCallback: WhisperProgressCallback
    ): String

    external fun segmentCount(modelHandle: Long): Int

    external fun segmentText(
        modelHandle: Long,
        segmentIndex: Int
    ): String

    external fun segmentStartMilliseconds(
        modelHandle: Long,
        segmentIndex: Int
    ): Long

    external fun segmentEndMilliseconds(
        modelHandle: Long,
        segmentIndex: Int
    ): Long

    @Suppress("unused")
    external fun freeModel(modelHandle: Long)

    private companion object {
        init {
            System.loadLibrary("sparrow_voice")
        }
    }
}
