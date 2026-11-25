package com.cbgm.sparrow.feature.embedding.data.platform

interface EmbeddingModelLocalDataSource {
    suspend fun isModelReady(): Boolean

    suspend fun downloadAndVerify(onProgress: (Float?) -> Unit)

    suspend fun deleteModel()
}
