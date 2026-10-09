package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseMember
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalIdentityNameUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.flow.first

/** Group membership's installed keys identify participants; contact verification is not required. */
class GetGroupExpenseCreationContextUseCase(
    private val pins: GroupPinRepository,
    private val membership: GroupMembershipRepository,
    private val contacts: ContactRepository,
    private val localIdentity: LocalPublicIdentityProvider,
    private val getLocalIdentityName: GetLocalIdentityNameUseCase
) {
    suspend operator fun invoke(groupId: String): Result<GroupExpenseCreationContext> = safeSuspendCall {
        require(groupId.isNotBlank()) { "Group ID is missing" }
        val board = pins.observe(groupId).first()?.message?.parts
            ?.filterIsInstance<ExpenseBoard>()
            ?.singleOrNull { it.closedAtEpochMilliseconds == null }
            ?: error("Group expenses are not active")

        val administration = membership.observeAdministration(groupId).first { it.activeMemberCount > 0 }
        val routingMembers = membership.getCurrentTransportRoutingMembers(groupId).getOrThrow().orEmpty()
            .associateBy { it.contactId }
        val contactsById = contacts.observeContacts().first().associateBy { it.id }
        val localId = localIdentity.getLocalPublicIdentity().getOrThrow().signingPublicKey.toExpenseMemberId()
        val localName = getLocalIdentityName().getOrNull().orEmpty()
        val members = buildList {
            add(GroupExpenseMember(localId, localName, true))
            administration.currentMemberContactIds.sorted().forEach { contactId ->
                // A group member does not need to have a manually verified Contact identity.
                // Only membership's current epoch keys are safe to persist in expense attachments.
                val signingKey = routingMembers[contactId]?.signingPublicKey ?: return@forEach
                val name = contactsById[contactId]?.displayName?.takeIf(String::isNotBlank) ?: contactId
                val id = signingKey.toExpenseMemberId()
                if (id != localId && none { it.id == id }) add(GroupExpenseMember(id, name, false, contactId))
            }
        }
        GroupExpenseCreationContext(board, localId, members)
    }
}

private fun ByteArray.toExpenseMemberId(): String {
    require(isNotEmpty()) { "Missing group signing key" }
    val hex = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toExpenseMemberId) {
            val value = byte.toInt() and 0xff
            append(hex[value ushr 4])
            append(hex[value and 15])
        }
    }
}
