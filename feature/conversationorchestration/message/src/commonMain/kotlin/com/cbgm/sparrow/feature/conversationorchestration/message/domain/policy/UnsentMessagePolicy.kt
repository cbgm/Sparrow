package com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy

import com.cbgm.sparrow.feature.transport.blob.isFileTransferFailure
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute

/**
 * A message whose attachment could not go out yet is kept to be sent again instead of being thrown away: the user
 * chose to wait for a protected route, or the upload failed. Both pass; anything else stays a broken message.
 */
object UnsentMessagePolicy {
    fun keepsMessage(cause: Throwable): Boolean = cause.isWaitingForRoute() || cause.isFileTransferFailure()
}
