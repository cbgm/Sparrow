package com.cbgm.sparrow.feature.search.data.platform

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MediaPipeLocalTextEmbedder(
    private val context: Context,
    private val modelManager: AndroidSemanticSearchModelManager
) : LocalTextEmbedder {
    private var textEmbedder: TextEmbedder? = null
    private var mappedModel: MappedByteBuffer? = null

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
        mappedModel = null
    }

    private fun requireEmbedder(): TextEmbedder {
        textEmbedder?.let { return it }
        val modelFile = modelManager.requireVerifiedModelFile()
        val mapped =
            FileInputStream(modelFile).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            }
        mappedModel = mapped
        val baseOptions = BaseOptions.builder().setModelAssetBuffer(mapped).build()
        val options =
            TextEmbedder.TextEmbedderOptions
                .builder()
                .setBaseOptions(baseOptions)
                .build()
        return TextEmbedder.createFromOptions(context, options).also { textEmbedder = it }
    }
}
