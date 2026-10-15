package com.cbgm.sparrow.feature.ocr.device

import android.content.Context
import android.net.Uri
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidTextRecognizer(
    private val context: Context
) : TextRecognizer {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(localFilePath: String): Result<List<RecognizedTextLine>> =
        safeSuspendCall {
            val image =
                withContext(Dispatchers.IO) {
                    InputImage.fromFilePath(context, Uri.fromFile(File(localFilePath)))
                }
            recognizer.process(image).await().toRecognizedTextLines()
        }

    private fun Text.toRecognizedTextLines(): List<RecognizedTextLine> =
        textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            RecognizedTextLine(
                text = line.text,
                left = box.left.toFloat(),
                top = box.top.toFloat(),
                right = box.right.toFloat(),
                bottom = box.bottom.toFloat()
            )
        }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result -> continuation.resume(result) }
            addOnFailureListener { error -> continuation.resumeWithException(error) }
            addOnCanceledListener { continuation.cancel() }
        }
}
