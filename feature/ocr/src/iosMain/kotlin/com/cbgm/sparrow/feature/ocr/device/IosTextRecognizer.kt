package com.cbgm.sparrow.feature.ocr.device

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

@OptIn(ExperimentalForeignApi::class)
internal class IosTextRecognizer : TextRecognizer {
    override suspend fun recognize(localFilePath: String): Result<List<RecognizedTextLine>> =
        safeSuspendCall {
            withContext(Dispatchers.Default) {
                val request = VNRecognizeTextRequest()
                request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
                request.usesLanguageCorrection = true
                val handler =
                    VNImageRequestHandler(
                        uRL = NSURL.fileURLWithPath(localFilePath),
                        options = emptyMap<Any?, Any?>()
                    )
                check(handler.performRequests(listOf(request), error = null)) {
                    "Text recognition failed"
                }
                request.results.orEmpty().mapNotNull { result ->
                    (result as? VNRecognizedTextObservation)?.toRecognizedTextLine()
                }
            }
        }

    private fun VNRecognizedTextObservation.toRecognizedTextLine(): RecognizedTextLine? {
        val text = (topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string ?: return null
        return boundingBox.useContents {
            RecognizedTextLine(
                text = text,
                left = origin.x.toFloat(),
                top = (1.0 - origin.y - size.height).toFloat(),
                right = (origin.x + size.width).toFloat(),
                bottom = (1.0 - origin.y).toFloat()
            )
        }
    }
}
