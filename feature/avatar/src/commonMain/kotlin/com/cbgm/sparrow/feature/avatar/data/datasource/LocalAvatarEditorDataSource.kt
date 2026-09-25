package com.cbgm.sparrow.feature.avatar.data.datasource

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult

internal class LocalAvatarEditorDataSource {
    private var sourceBytes: ByteArray? = null
    private val results = mutableMapOf<Long, ByteArray>()
    private var nextResultId = 1L

    fun setSource(bytes: ByteArray) {
        sourceBytes = bytes.copyOf()
    }

    fun getSource(): ByteArray? = sourceBytes?.copyOf()

    fun clearSource() {
        sourceBytes = null
    }

    fun saveResult(bytes: ByteArray): AvatarEditResult {
        val result = AvatarEditResult(nextResultId++)
        results[result.id] = bytes.copyOf()
        return result
    }

    fun consumeResult(result: AvatarEditResult): ByteArray? =
        results.remove(result.id)?.copyOf()
}
