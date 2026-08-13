package com.cbgm.securechat.feature.contacts.data.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInvitePacket
import com.cbgm.securechat.core.protocol.packet.ContactReadyPacket
import com.cbgm.securechat.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.data.repository.IdentityInvitationRepositoryImpl

class ContactInvitePacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveInvite(
            context = context,
            packet = packet as? ContactInvitePacket ?: error("Incompatible contact invite packet")
        )
}

class ContactInviteAcceptedPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveAccepted(
            context = context,
            packet = packet as? ContactInviteAcceptedPacket ?: error("Incompatible contact acceptance packet")
        )
}

class ContactReadyPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactReadyPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveReady(
            context = context,
            packet = packet as? ContactReadyPacket ?: error("Incompatible contact ready packet")
        )
}

class ContactInviteDeclinedPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        coordinator.receiveDeclined(
            context = context,
            packet = packet as? ContactInviteDeclinedPacket ?: error("Incompatible contact decline packet")
        )
}

class DirectChatAuthorizationRevokedPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is DirectChatAuthorizationRevokedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val authorizationError =
                coordinator
                    .receiveDirectChatAuthorizationRevoked(
                        context = context,
                        packet =
                            packet as? DirectChatAuthorizationRevokedPacket
                                ?: error("Incompatible direct chat authorization revocation packet")
                    ).exceptionOrNull()
            val mailboxError =
                mailboxCapabilityLifecycle.revokeForContact(context.contactId).exceptionOrNull()
            authorizationError?.let { throw it }
            mailboxError?.let { throw it }
        }
}
