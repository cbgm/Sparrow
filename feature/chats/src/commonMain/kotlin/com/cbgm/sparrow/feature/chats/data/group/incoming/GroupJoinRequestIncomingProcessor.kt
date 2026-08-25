package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipActivationCoordinator
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.EstablishGroupMemberIdentityUseCase

internal class GroupJoinRequestIncomingProcessor(
    private val groupInvitationDao: GroupInvitationDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val activation: GroupMembershipActivationCoordinator,
    private val establishGroupMemberIdentity: EstablishGroupMemberIdentityUseCase
) {
    suspend fun process(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")

            check(invitation.groupId == packet.groupId) { "Join request uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Join request came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Join request challenge does not match" }
            check(receivedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                "Group invitation has expired"
            }

            membershipPacketProtocol.verifyJoinRequest(packet).getOrThrow()
            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                return@runCatching
            }

            establishGroupMemberIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            ).getOrThrow()

            if (
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                invitation.status == GroupInvitationStatus.INVITE_RECEIVED.name ||
                invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name
            ) {
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = invitation.status,
                        newStatus =
                            GroupMembershipStateMachine
                                .transition(
                                    invitation.status,
                                    GroupMembershipEvent.IDENTITY_CONFIRMED
                                ).name,
                        updatedAt =
                            resolveInvitationUpdatedAt(
                                createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                                candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                            )
                    )
                check(updated == 1) { "Group invitation changed while the join request was applied" }
            } else {
                check(invitation.status == GroupInvitationStatus.IDENTITY_READY.name) {
                    "Unsupported group invitation status: ${invitation.status}"
                }
            }

            activation.activateGroupIfReady(packet.groupId).getOrThrow()
        }
}
