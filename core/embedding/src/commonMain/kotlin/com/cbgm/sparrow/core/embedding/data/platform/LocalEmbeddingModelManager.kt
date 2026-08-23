package com.cbgm.sparrow.core.embedding.data.platform

interface LocalEmbeddingModelManager {
    suspend fun isModelReady(): Boolean

    suspend fun downloadAndVerify(onProgress: (Float?) -> Unit)

    suspend fun deleteModel()
}
