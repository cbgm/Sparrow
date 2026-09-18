package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipActivationCoordinator
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

internal class GroupJoinRequestIncomingProcessorImpl(
    private val contactDao: ContactDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val membershipAttempts: GroupMembershipAttemptDataSource,
    private val activation: GroupMembershipActivationCoordinator
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<InvitationResponse?> =
        runCatching {
            val membership =
                membershipAttempts.findBySourceInvitationId(packet.invitationId)
                    ?: error("Group membership attempt was not found")

            check(membership.groupId == packet.groupId) { "Membership uses the wrong group" }
            check(membership.contactId == memberContactId) { "Membership belongs to the wrong contact" }
            check(membership.challenge.contentEquals(packet.challenge)) { "Join request challenge does not match" }

            membershipPacketProtocol.verifyJoinRequest(packet).getOrThrow()
            if (
                membership.status == GroupMembershipStatus.WELCOME_SENT ||
                membership.status == GroupMembershipStatus.ACTIVE
            ) {
                return@runCatching InvitationResponse.ACCEPTED
            }

            establishMemberIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )

            when (membership.status) {
                GroupMembershipStatus.STAGED ->
                    membershipAttempts
                        .markIdentityConfirmed(
                            sourceInvitationId = membership.sourceInvitationId,
                            updatedAtEpochMilliseconds = receivedAtEpochMilliseconds
                        ).getOrThrow()

                GroupMembershipStatus.IDENTITY_READY -> Unit
                else -> error("Unsupported group membership status: ${membership.status}")
            }

            activation.activateGroupIfReady(packet.groupId).getOrThrow()
            InvitationResponse.ACCEPTED
        }

    private suspend fun establishMemberIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        val existingIdentity = contactDao.findPublicIdentityByContactId(contactId)
        when {
            existingIdentity == null ||
                !existingIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey) ||
                !existingIdentity.signingPublicKey.contentEquals(signingPublicKey) ->
                contactKeyExchangeDataSource.prepareRemoteIdentityForHandshake(
                    contactId = contactId,
                    remoteEncryptionPublicKey = encryptionPublicKey,
                    remoteSigningPublicKey = signingPublicKey
                )

            existingIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name ->
                contactKeyExchangeDataSource.acceptRemoteIdentityForHandshake(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                )
        }

        contactKeyExchangeDataSource.markMutual(
            contactId = contactId,
            expectedRemoteEncryptionPublicKey = encryptionPublicKey,
            expectedRemoteSigningPublicKey = signingPublicKey
        )
    }
}
