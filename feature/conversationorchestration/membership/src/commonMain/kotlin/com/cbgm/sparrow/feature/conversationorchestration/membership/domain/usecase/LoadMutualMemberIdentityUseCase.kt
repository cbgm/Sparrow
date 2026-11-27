package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/** The accepted member's identity for welcome recovery; only a mutual key exchange may be bound to the group. */
class LoadMutualMemberIdentityUseCase(
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(contactId: String): Result<RemotePeerIdentity> =
        safeSuspendCall(logFailure = false) {
            val identity =
                requireNotNull(getRemoteIdentity(contactId).getOrThrow()) {
                    "Accepted group member identity was not found for welcome recovery"
                }
            check(identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                "Group member identity exchange is not mutual"
            }
            identity
        }
}
