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

/** Resolves group members to stable public signing-key identifiers, never device-local contact IDs. */
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
        val contactsById = contacts.observeContacts().first().associateBy { it.id }
        val localKey = localIdentity.getLocalPublicIdentity().getOrThrow().signingPublicKey
        val localId = localKey.toExpenseMemberId()
        val ownName = getLocalIdentityName().getOrNull().orEmpty()
        val otherMembers = administration.currentMemberContactIds.sorted().map { contactId ->
            val contact = contactsById[contactId] ?: error("A group member has no contact record")
            val signingKey = contact.sparrowIdentity?.signingPublicKey
                ?: error("A group member has no verified public identity")
            GroupExpenseMember(signingKey.toExpenseMemberId(), contact.displayName?.takeIf(String::isNotBlank) ?: contactId, false)
        }
        val members = listOf(GroupExpenseMember(localId, ownName, true)) + otherMembers
        check(members.map { it.id }.distinct().size == members.size) { "Duplicate group member identity" }
        GroupExpenseCreationContext(board, localId, members)
    }
}

private fun ByteArray.toExpenseMemberId(): String {
    require(isNotEmpty()) { "Missing signing key" }
    val hex = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toExpenseMemberId) {
            val value = byte.toInt() and 0xff
            append(hex[value ushr 4])
            append(hex[value and 15])
        }
    }
}
