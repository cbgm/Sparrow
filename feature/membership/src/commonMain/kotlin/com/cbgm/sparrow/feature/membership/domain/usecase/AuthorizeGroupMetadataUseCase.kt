package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataSendContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Read-only security decision: no chat persistence and no cross-module workflow. */
class AuthorizeGroupMetadataUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend fun resolveMessageSender(
        groupId: String,
        epoch: Int,
        signingPublicKey: ByteArray
    ): Result<GroupMetadataMessageSender> =
        repository.resolveMetadataMessageSender(groupId, epoch, signingPublicKey)

    suspend fun send(
        groupId: String,
        localSigningPublicKey: ByteArray,
        action: String
    ): Result<GroupMetadataSendContext> =
        repository.authorizeMetadataSend(groupId, localSigningPublicKey, action)

    suspend fun receive(
        groupId: String,
        epoch: Int,
        contactId: String,
        adminSigningPublicKey: ByteArray,
        action: String
    ): Result<Boolean> =
        repository.authorizeMetadataReceive(groupId, epoch, contactId, adminSigningPublicKey, action)
}
