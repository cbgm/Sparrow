package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository

class HandleDirectChatAuthorizationRevokedPacketUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> =
        runCatching {
            val authorizationError =
                directIdentityExchangeRepository
                    .receiveDirectChatAuthorizationRevoked(context, packet)
                    .exceptionOrNull()
            val mailboxError =
                mailboxCapabilityLifecycle
                    .revokeForContact(context.contactId)
                    .exceptionOrNull()

            authorizationError?.let { throw it }
            mailboxError?.let { throw it }
        }
}
