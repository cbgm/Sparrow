package com.cbgm.sparrow.feature.search.data.model

internal object SemanticSearchModel {
    const val VERSION = 1
    const val OUTPUT_DIMENSIONS = 128
    const val INDEX_BATCH_SIZE = 32
    const val MODEL_FILE_NAME = "embedding_gemma.task"
    const val MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/text_embedder/embedding_gemma/int4int8/latest/embedding_gemma.task"
}
