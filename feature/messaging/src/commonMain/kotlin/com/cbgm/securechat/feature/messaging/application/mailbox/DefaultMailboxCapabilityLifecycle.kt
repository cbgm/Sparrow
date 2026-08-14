package com.cbgm.securechat.feature.messaging.application.mailbox

import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.feature.transport.mailbox.MailboxGateway

class DefaultMailboxCapabilityLifecycle(
    private val repository: MailboxRouteRepository,
    private val gateway: MailboxGateway
) : MailboxCapabilityLifecycle {
    override suspend fun revokeForContact(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            repository.deleteRemote(contactId).getOrThrow()
            val credential = repository.localForContact(contactId).getOrThrow() ?: return@runCatching
            repository.markLocalRevocationPending(contactId).getOrThrow()
            revokeAndDelete(credential)
        }

    override suspend fun revokeAll(): Result<Unit> =
        runCatching {
            repository.deleteAllRemote().getOrThrow()
            val failures = mutableListOf<Throwable>()
            repository.allLocal().getOrThrow().forEach { credential ->
                repository.markLocalRevocationPending(credential.contactId).getOrThrow()
                runCatching { revokeAndDelete(credential) }
                    .onFailure(failures::add)
            }
            if (failures.isNotEmpty()) {
                throw IllegalStateException(
                    "${failures.size} mailbox capability revocation(s) remain pending",
                    failures.first()
                )
            }
        }

    override suspend fun retryPendingRevocations(): Result<Int> =
        runCatching {
            var revoked = 0
            repository
                .allLocal()
                .getOrThrow()
                .filter(LocalMailboxCredential::revocationPending)
                .forEach { credential ->
                    revokeAndDelete(credential)
                    revoked += 1
                }
            revoked
        }

    private suspend fun revokeAndDelete(credential: LocalMailboxCredential) {
        gateway.revoke(credential).getOrThrow()
        repository.deleteLocal(credential.contactId).getOrThrow()
    }
}
