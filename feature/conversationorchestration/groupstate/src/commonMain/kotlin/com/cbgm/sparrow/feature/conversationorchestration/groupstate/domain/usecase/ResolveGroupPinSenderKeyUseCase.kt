package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupPinSenderSigningKeyUseCase

/**
 * The signing key of the member who wrote a pinned message, so every member can verify the pin snapshot.
 * Membership knows the epoch key of current members; an earlier sender falls back to their pinned identity.
 */
class ResolveGroupPinSenderKeyUseCase(
    private val getGroupPinSenderSigningKey: GetGroupPinSenderSigningKeyUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(groupId: String, message: StoredMessage): Result<ByteArray> =
        safeSuspendCall(logFailure = false) {
            val memberKey =
                getGroupPinSenderSigningKey(
                    groupId = groupId,
                    isMine = message.isMine,
                    senderContactId = message.senderContactId
                ).getOrThrow()
            val senderKey = memberKey ?: run {
                val contactId =
                    requireNotNull(message.senderContactId) { "Incoming group message has no sender contact" }
                getRemoteIdentity(contactId).getOrThrow()
                    ?.signingPublicKey
                    ?.copyOf()
                    ?: error("Pinned message sender identity was not found")
            }
            require(senderKey.isNotEmpty()) { "Pinned message sender identity was not found" }
            senderKey
        }
}
