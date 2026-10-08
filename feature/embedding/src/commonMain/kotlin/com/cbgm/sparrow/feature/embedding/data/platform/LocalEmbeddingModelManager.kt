package com.cbgm.sparrow.feature.embedding.data.platform

interface LocalEmbeddingModelManager {
    suspend fun isModelReady(): Boolean

    suspend fun downloadAndVerify(onProgress: (Float?) -> Unit)

    suspend fun deleteModel()
}
