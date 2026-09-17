package com.cbgm.sparrow.feature.invite.data.group

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.InvitationDao
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.membership.data.coordinator.GroupMembershipActivationCoordinator
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipAttemptDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

internal class GroupJoinRequestIncomingProcessor(
    private val invitationDao: InvitationDao,
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
    ): Result<Unit> =
        runCatching {
            val membership =
                membershipAttempts.findBySourceInvitationId(packet.invitationId)
                    ?: error("Group membership attempt was not found")
            val invitation =
                invitationDao.findById(packet.invitationId)
                    ?: error("Group invitation was not found")

            check(invitation.payloadType == InvitationPayloadType.GROUP.name) { "Invitation is not a group invitation" }
            check(invitation.payloadId == packet.groupId) { "Join request uses the wrong group" }
            check(invitation.peerId == memberContactId) { "Join request came from the wrong contact" }
            check(receivedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                "Group invitation has expired"
            }
            check(membership.groupId == packet.groupId) { "Membership uses the wrong group" }
            check(membership.contactId == memberContactId) { "Membership belongs to the wrong contact" }
            check(membership.challenge.contentEquals(packet.challenge)) { "Join request challenge does not match" }

            membershipPacketProtocol.verifyJoinRequest(packet).getOrThrow()
            if (
                membership.status == GroupMembershipStatus.WELCOME_SENT ||
                membership.status == GroupMembershipStatus.ACTIVE
            ) {
                markInvitationAcceptedIfPending(
                    invitationId = membership.sourceInvitationId,
                    updatedAt = receivedAtEpochMilliseconds
                )
                return@runCatching
            }

            establishMemberIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )

            when (membership.status) {
                GroupMembershipStatus.STAGED -> {
                    val updated =
                        membershipAttempts
                            .markIdentityConfirmed(
                                sourceInvitationId = membership.sourceInvitationId,
                                updatedAtEpochMilliseconds = receivedAtEpochMilliseconds
                            ).getOrThrow()
                    markInvitationAcceptedIfPending(
                        invitationId = membership.sourceInvitationId,
                        updatedAt = updated.updatedAtEpochMilliseconds
                    )
                }

                GroupMembershipStatus.IDENTITY_READY ->
                    markInvitationAcceptedIfPending(
                        invitationId = membership.sourceInvitationId,
                        updatedAt = receivedAtEpochMilliseconds
                    )

                else -> error("Unsupported group membership status: ${membership.status}")
            }

            activation.activateGroupIfReady(packet.groupId).getOrThrow()
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

    private suspend fun markInvitationAcceptedIfPending(
        invitationId: String,
        updatedAt: Long
    ) {
        val invitation = invitationDao.findById(invitationId) ?: return
        if (invitation.payloadType != InvitationPayloadType.GROUP.name) return
        if (invitation.status != INVITATION_STATUS_PENDING) return
        invitationDao.updateStatus(
            invitationId = invitationId,
            expectedStatus = INVITATION_STATUS_PENDING,
            newStatus = INVITATION_STATUS_ACCEPTED,
            updatedAt = maxOf(invitation.createdAtEpochMilliseconds, updatedAt)
        )
    }

    private companion object {
        const val INVITATION_STATUS_PENDING = "PENDING"
        const val INVITATION_STATUS_ACCEPTED = "ACCEPTED"
    }
}
