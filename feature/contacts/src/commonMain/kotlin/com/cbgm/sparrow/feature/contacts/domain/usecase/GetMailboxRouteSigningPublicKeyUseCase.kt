package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

/**
 * The key a contact's mailbox route must be signed with: the mutual identity key, or, right after the handshake and
 * before the identity is verified, the key the handshake established. That key already protects every message of
 * the contact, so a route signed with it is as trustworthy as those messages.
 */
class GetMailboxRouteSigningPublicKeyUseCase(
    private val repository: ContactTransportRepository,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend operator fun invoke(contactId: String): Result<ByteArray?> =
        safeSuspendCall(logFailure = false) {
            repository.getMutualSigningPublicKey(contactId).getOrThrow()
                ?: repository
                    .getSigningPublicKey(contactId)
                    .getOrThrow()
                    ?.takeIf { getIdentityPeerState(contactId).getOrThrow().hasEstablishedExchange }
        }
}
