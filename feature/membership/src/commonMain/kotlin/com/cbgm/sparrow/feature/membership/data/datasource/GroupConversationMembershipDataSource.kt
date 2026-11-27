package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.protocol.membership.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.protocol.membership.GroupConversationMembershipSource
import com.cbgm.sparrow.protocol.membership.GroupMemberLifecycleSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class GroupConversationMembershipDataSource(
    private val securityStore: GroupSecurityStoreDataSource,
    private val membershipStore: GroupMembershipStoreDataSource
) : GroupConversationMembershipSource {
    override fun observeConversationMembership(groupId: String): Flow<GroupConversationMembershipSnapshot> =
        combine(
            securityStore.observeCurrentMemberKeys(groupId),
            membershipStore.observeByGroupId(groupId)
        ) { currentMemberKeys, memberships ->
            GroupConversationMembershipSnapshot(
                participantContactIds = currentMemberKeys.map { it.contactId },
                memberships = memberships.map { membership ->
                    GroupMemberLifecycleSnapshot(
                        contactId = membership.contactId,
                        sourceInvitationId = membership.sourceInvitationId,
                        status = membership.status,
                        createdAtEpochMilliseconds = membership.createdAtEpochMilliseconds
                    )
                }
            )
        }
}
