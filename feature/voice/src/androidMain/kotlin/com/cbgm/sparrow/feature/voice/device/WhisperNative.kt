package com.cbgm.sparrow.feature.voice.device

internal class WhisperNative {
    external fun loadModel(modelPath: String): Long

    external fun transcribe(
        modelHandle: Long,
        samples: FloatArray
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

    external fun freeModel(modelHandle: Long)

    private companion object {
        init {
            System.loadLibrary("sparrow_voice")
        }
    }
}
