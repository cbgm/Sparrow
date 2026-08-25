package com.cbgm.sparrow.feature.attachments.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MessageAttachmentCacheCoordinator(
    private val attachmentDataSource: MessageAttachmentDataSource
) {
    private val logger = SparrowLog.withTag("MessageAttachmentCacheCoordinator")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeMessageIds = mutableSetOf<String>()
    private val mutex = Mutex()

    fun cache(messageId: String) {
        if (messageId.isBlank()) return

        scope.launch {
            val shouldCache = mutex.withLock { activeMessageIds.add(messageId) }
            if (!shouldCache) return@launch

            try {
                attachmentDataSource.cacheIncoming(messageId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                logger.warn(error) { "Could not cache attachments for message $messageId" }
            } finally {
                mutex.withLock { activeMessageIds.remove(messageId) }
            }
        }
    }
}
