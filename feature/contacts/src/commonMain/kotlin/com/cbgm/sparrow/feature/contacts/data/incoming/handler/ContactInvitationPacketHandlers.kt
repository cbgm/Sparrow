package com.cbgm.sparrow.feature.contacts.data.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityInvitationRepositoryImpl

class ContactInvitePacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveInvite(
            context = context,
            packet = packet as? ContactInvitePacket ?: error("Incompatible contact invite packet")
        )
}

class ContactInviteAcceptedPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveAccepted(
            context = context,
            packet = packet as? ContactInviteAcceptedPacket ?: error("Incompatible contact acceptance packet")
        )
}

class ContactReadyPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactReadyPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveReady(
            context = context,
            packet = packet as? ContactReadyPacket ?: error("Incompatible contact ready packet")
        )
}

class ContactInviteDeclinedPacketHandler(
    private val coordinator: IdentityInvitationRepositoryImpl
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
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
    override fun canHandle(packet: SparrowPacket): Boolean = packet is DirectChatAuthorizationRevokedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
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
