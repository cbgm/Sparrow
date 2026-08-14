package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.securechat.feature.chats.data.group.security.isGroupAdminRole

class GroupMemberActivationAcknowledgementPacketHandler(
    private val groupSecurityDao: GroupSecurityDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val membershipCoordinator: GroupMembershipCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupMemberActivationAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val acknowledgement =
                packet as? GroupMemberActivationAcknowledgementPacket
                    ?: error("GroupMemberActivationAcknowledgementPacketHandler received an incompatible packet")
            check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                "Group member activation acknowledgement requires encrypted transport"
            }

            val securityState =
                groupSecurityDao.findState(acknowledgement.groupId)
                    ?: error("Group security state was not found")
            check(securityState.currentEpoch == acknowledgement.epoch) {
                "Group member activation acknowledgement uses the wrong epoch"
            }
            check(securityState.localRole.isGroupAdminRole()) {
                "Only a group admin may receive member activation acknowledgements"
            }

            val acknowledgingMemberKey =
                groupSecurityDao.findMemberKey(
                    groupId = acknowledgement.groupId,
                    epoch = securityState.currentEpoch,
                    contactId = context.contactId
                ) ?: error("Acknowledging contact is not part of the current group epoch")
            check(
                acknowledgingMemberKey.signingPublicKey.contentEquals(
                    acknowledgement.acknowledgingMemberSigningPublicKey
                )
            ) {
                "Acknowledgement signing identity does not match the group member"
            }
            membershipPacketProtocol
                .verifyMemberActivationAcknowledgement(
                    packet = acknowledgement,
                    expectedMemberSigningPublicKey = acknowledgingMemberKey.signingPublicKey
                ).getOrThrow()
            membershipCoordinator
                .receiveMemberActivationAcknowledgement(
                    packet = acknowledgement,
                    acknowledgingContactId = context.contactId
                ).getOrThrow()
        }

    private companion object {
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
