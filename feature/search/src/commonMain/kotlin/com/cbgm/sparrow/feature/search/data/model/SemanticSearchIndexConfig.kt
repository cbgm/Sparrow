package com.cbgm.sparrow.feature.search.data.model

import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel

internal object SemanticSearchIndexConfig {
    /**
     * Version 2 embeds message metadata (sender and conversation) together with message text.
     * Changing the indexed representation must bump this version so the derived index rebuilds.
     */
    const val VERSION = 2
    const val EMBEDDING_DIMENSIONS = LocalEmbeddingModel.OUTPUT_DIMENSIONS
    const val BATCH_SIZE = 32
}
