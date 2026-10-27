package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository

/**
 * Revokes mailbox capabilities on their node. Once a revocation is marked pending it is never lost: a node that is
 * unreachable or refuses is retried by the provisioning, and the caller (deleting a chat, approving an identity
 * change) is not blocked by it.
 */
class DefaultMailboxCapabilityLifecycle(
    private val repository: MailboxRouteRepository,
    private val gateway: MailboxGateway
) : MailboxCapabilityLifecycle {
    private val logger = SparrowLog.withTag("DefaultMailboxCapabilityLifecycle")

    override suspend fun revokeForContact(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            repository.deleteRemote(contactId).getOrThrow()
            val credential = repository.localForContact(contactId).getOrThrow() ?: return@safeSuspendCall
            repository.markLocalRevocationPending(contactId).getOrThrow()
            revokeAndDelete(credential).onFailure { failure ->
                logger.warn(failure) { "Mailbox revocation for $contactId stays pending" }
            }
        }

    override suspend fun revokeAll(): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            repository.deleteAllRemote().getOrThrow()
            val failures =
                repository.allLocal().getOrThrow().mapNotNull { credential ->
                    repository.markLocalRevocationPending(credential.contactId).getOrThrow()
                    revokeAndDelete(credential).exceptionOrNull()
                }
            if (failures.isNotEmpty()) {
                throw IllegalStateException(
                    "${failures.size} mailbox capability revocation(s) remain pending",
                    failures.first()
                )
            }
        }

    /** Every pending revocation is tried; one that fails stays pending without holding up the others. */
    override suspend fun retryPendingRevocations(): Result<Int> =
        safeSuspendCall(logFailure = false) {
            repository
                .allLocal()
                .getOrThrow()
                .filter(LocalMailboxCredential::revocationPending)
                .count { credential ->
                    revokeAndDelete(credential)
                        .onFailure { failure ->
                            logger.warn(failure) { "Mailbox revocation for ${credential.contactId} stays pending" }
                        }.isSuccess
                }
        }

    private suspend fun revokeAndDelete(credential: LocalMailboxCredential): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            gateway.revoke(credential).getOrThrow()
            repository.deleteLocal(credential.contactId).getOrThrow()
        }
}
