package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupPinSenderSigningKeyUseCase

/** Resolve Membership/Identity inputs in the use case, never inside Chats' repository. */
class PinGroupMessageUseCase(
    private val repository: GroupPinRepository,
    private val getGroupPinSenderSigningKey: GetGroupPinSenderSigningKeyUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<Unit> =
        safeSuspendCall {
            val target = repository.getPinTarget(groupId, messageId).getOrThrow()
            if (target.isAlreadyPinned) {
                // Still perform the repository's admin check, but the existing pin has no need
                // to resolve another identity (matching the original early-return behavior).
                repository.pin(groupId, messageId, target, byteArrayOf()).getOrThrow()
                return@safeSuspendCall
            }
            val memberSigningKey = getGroupPinSenderSigningKey(
                groupId = groupId,
                isMine = target.isMine,
                senderContactId = target.senderContactId
            ).getOrThrow()
            val senderSigningKey = memberSigningKey ?: run {
                val contactId = requireNotNull(target.senderContactId) {
                    "Incoming group message has no sender contact"
                }
                getRemoteIdentity(contactId).getOrThrow()
                    ?.signingPublicKey
                    ?.copyOf()
                    ?: error("Pinned message sender identity was not found")
            }
            repository.pin(
                groupId = groupId,
                messageId = messageId,
                target = target,
                senderSigningPublicKey = senderSigningKey
            ).getOrThrow()
        }
}
