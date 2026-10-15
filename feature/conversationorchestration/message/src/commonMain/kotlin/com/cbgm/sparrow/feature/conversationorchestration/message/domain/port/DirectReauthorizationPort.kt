package com.cbgm.sparrow.feature.conversationorchestration.message.domain.port

/**
 * Starts a fresh invitation when a direct message finds no usable authorization.
 * Implemented by the invite workflows; declared here so messages do not depend on invitations.
 */
interface DirectReauthorizationPort {
    suspend fun requestReauthorization(contactId: String): Result<Unit>
}
