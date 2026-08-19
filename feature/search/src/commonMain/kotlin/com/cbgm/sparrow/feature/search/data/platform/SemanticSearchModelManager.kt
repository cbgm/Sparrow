package com.cbgm.sparrow.feature.search.data.platform

interface SemanticSearchModelManager {
    suspend fun isModelReady(): Boolean

    suspend fun downloadAndVerify(onProgress: (Float?) -> Unit)

    suspend fun deleteModel()
}
