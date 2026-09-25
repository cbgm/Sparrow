package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

/** Membership alone persists group epoch, member keys and the secured welcome. */
class PersistIncomingGroupWelcomeUseCase(
    private val security: GroupSecurityRepository
) {
    suspend operator fun invoke(
        welcome: OpenedIncomingGroupWelcome,
        ownerContactId: String,
        memberKeys: List<GroupWelcomeMemberKey>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = security.persistJoinedGroup(
        openedWelcome = welcome.openedWelcome,
        ownerContactId = ownerContactId,
        authoritySigningPublicKey = welcome.authoritySigningPublicKey,
        localSigningPublicKey = welcome.localSigningPublicKey,
        memberKeys = memberKeys,
        receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
    )
}
