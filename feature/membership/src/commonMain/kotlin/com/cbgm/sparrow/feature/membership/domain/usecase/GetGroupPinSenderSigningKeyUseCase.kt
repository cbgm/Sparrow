package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Read-only Membership security lookup, with no dependency on Chats or Contacts. */
class GetGroupPinSenderSigningKeyUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        isMine: Boolean,
        senderContactId: String?
    ): Result<ByteArray?> = repository.resolvePinnedSenderSigningKey(groupId, isMine, senderContactId)
}
