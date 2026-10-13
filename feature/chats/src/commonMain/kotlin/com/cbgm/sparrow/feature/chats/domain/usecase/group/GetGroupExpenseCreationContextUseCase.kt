package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.core.messagepart.domain.model.expenseId
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseCreationContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupExpenseMember
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.expenses.domain.ExpenseRevisions
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalIdentityNameUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.flow.first

/** Group membership's installed keys identify participants; contact verification is not required. */
class GetGroupExpenseCreationContextUseCase(
    private val pins: GroupPinReadRepository,
    private val membership: GroupMembershipRepository,
    private val contacts: ContactRepository,
    private val localIdentity: LocalPublicIdentityProvider,
    private val getLocalIdentityName: GetLocalIdentityNameUseCase,
    private val attachments: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(
        groupId: String,
        expenseId: String? = null
    ): Result<GroupExpenseCreationContext> = safeSuspendCall {
        require(groupId.isNotBlank()) { "Group ID is missing" }
        val board = pins.observe(groupId).first()?.message?.parts
            ?.filterIsInstance<ExpenseBoard>()
            ?.singleOrNull { it.closedAtEpochMilliseconds == null }
            ?: error("Group expenses are not active")

        val administration = membership.observeAdministration(groupId).first { it.activeMemberCount > 0 }
        val routingMembers = membership.getCurrentTransportRoutingMembers(groupId).getOrThrow().orEmpty()
            .associateBy { it.contactId }
        val contactsById = contacts.observeContacts().first().associateBy { it.id }
        val localId = localIdentity.getLocalPublicIdentity().getOrThrow().signingPublicKey.let(ExpenseMemberId::fromSigningPublicKey)
        val localName = getLocalIdentityName().getOrNull().orEmpty()
        val members = buildList {
            add(GroupExpenseMember(localId, localName, true))
            administration.currentMemberContactIds.sorted().forEach { contactId ->
                // A group member does not need to have a manually verified Contact identity.
                // Only membership's current epoch keys are safe to persist in expense attachments.
                val signingKey = routingMembers[contactId]?.signingPublicKey ?: return@forEach
                val name = contactsById[contactId]?.displayName?.takeIf(String::isNotBlank) ?: contactId
                val id = ExpenseMemberId.fromSigningPublicKey(signingKey)
                if (id != localId && none { it.id == id }) add(GroupExpenseMember(id, name, false, contactId))
            }
        }
        val expense = expenseId?.let { id ->
            ExpenseRevisions.current(attachments.observeExpenses(groupId).first())
                .firstOrNull { it.expenseId == id && it.boardId == board.id }
                ?: error("Expense was not found")
        }
        GroupExpenseCreationContext(board, localId, members, expense, administration.isLocalAdmin)
    }
}
