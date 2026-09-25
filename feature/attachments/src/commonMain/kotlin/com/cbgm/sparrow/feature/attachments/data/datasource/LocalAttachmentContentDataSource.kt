package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.feature.attachments.data.model.AttachmentContentPayloadDto
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentTargetDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LocalAttachmentContentDataSource {
    private val mutex = Mutex()
    private val entries = mutableMapOf<AttachmentTargetDto, AttachmentContentPayloadDto>()

    suspend fun get(target: AttachmentTargetDto): AttachmentContentPayloadDto? =
        mutex.withLock { entries[target] }

    suspend fun save(target: AttachmentTargetDto, content: AttachmentContentPayloadDto) {
        mutex.withLock { entries[target] = content }
    }

    suspend fun removeAttachmentIds(attachmentIds: Set<String>) {
        if (attachmentIds.isEmpty()) return
        mutex.withLock { entries.keys.removeAll { target -> target.id in attachmentIds } }
    }

    suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}
