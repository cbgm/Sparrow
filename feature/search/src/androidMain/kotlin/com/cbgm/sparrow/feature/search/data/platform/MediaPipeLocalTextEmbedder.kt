package com.cbgm.sparrow.feature.search.data.platform

import android.content.Context
import android.os.ParcelFileDescriptor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPipeLocalTextEmbedder(
    private val context: Context,
    private val modelManager: AndroidSemanticSearchModelManager
) : LocalTextEmbedder {
    private var textEmbedder: TextEmbedder? = null
    private var modelFileDescriptor: ParcelFileDescriptor? = null

    override suspend fun embed(
        text: String,
        inputType: EmbeddingInputType
    ): FloatArray =
        withContext(Dispatchers.Default) {
            val formatted =
                when (inputType) {
                    EmbeddingInputType.QUERY -> "task: search result | query: $text"
                    EmbeddingInputType.DOCUMENT -> "title: none | text: $text"
                }
            val result = requireEmbedder().embed(formatted)
            result.embeddingResult().embeddings().first().floatEmbedding()
        }

    override fun close() {
        textEmbedder?.close()
        textEmbedder = null
        modelFileDescriptor?.close()
        modelFileDescriptor = null
    }

    private fun requireEmbedder(): TextEmbedder {
        textEmbedder?.let { return it }

        val modelFile = modelManager.requireVerifiedModelFile()
        val descriptor =
            ParcelFileDescriptor.open(
                modelFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

        return try {
            val baseOptions =
                BaseOptions
                    .builder()
                    .setModelAssetFileDescriptor(descriptor.fd)
                    .build()
            val options =
                TextEmbedder.TextEmbedderOptions
                    .builder()
                    .setBaseOptions(baseOptions)
                    .build()

            TextEmbedder.createFromOptions(context, options).also { embedder ->
                modelFileDescriptor = descriptor
                textEmbedder = embedder
            }
        } catch (throwable: Throwable) {
            descriptor.close()
            throw throwable
        }
    }
}
