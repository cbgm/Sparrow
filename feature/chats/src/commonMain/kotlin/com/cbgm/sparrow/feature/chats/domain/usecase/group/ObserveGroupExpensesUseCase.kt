package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseGroup
import com.cbgm.sparrow.feature.expenses.domain.model.ExpenseMember
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalIdentityNameUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGroupExpensesUseCase(
    private val pins: GroupPinReadRepository,
    private val membership: GroupMembershipRepository,
    private val contacts: ContactRepository,
    private val localIdentity: LocalPublicIdentityProvider,
    private val getLocalIdentityName: GetLocalIdentityNameUseCase,
    private val attachments: MessageAttachmentOperationsRepository
) {
    operator fun invoke(groupId: String): Flow<ExpenseGroup?> =
        pins.observe(groupId)
            .map { pin ->
                pin?.message?.parts
                    ?.filterIsInstance<ExpenseBoard>()
                    ?.singleOrNull { board -> board.closedAtEpochMilliseconds == null }
            }.distinctUntilChanged()
            .flatMapLatest { board ->
                if (board == null) return@flatMapLatest flowOf(null)
                flow {
                    val localMemberId = localIdentity.getLocalPublicIdentity().getOrNull()
                        ?.signingPublicKey
                        ?.let(ExpenseMemberId::fromSigningPublicKey)
                    emitAll(
                        combine(
                            membership.observeAdministration(groupId).filter { it.activeMemberCount > 0 },
                            contacts.observeContacts(),
                            attachments.observeExpenseState(groupId)
                        ) { administration, currentContacts, parts ->
                            ExpenseGroup(
                                groupId = groupId,
                                board = board,
                                localMemberId = localMemberId,
                                isLocalAdmin = administration.isLocalAdmin,
                                members = members(
                                    groupId,
                                    localMemberId,
                                    administration.currentMemberContactIds,
                                    currentContacts.associate { it.id to it.displayName.orEmpty() }
                                ),
                                expenses = parts.filterIsInstance<Expense>().filter { it.boardId == board.id },
                                transfers = parts.filterIsInstance<PaymentTransfer>().filter { it.boardId == board.id }
                            )
                        }
                    )
                }
            }

    private suspend fun members(
        groupId: String,
        localMemberId: String?,
        contactIds: Collection<String>,
        contactNames: Map<String, String>
    ): List<ExpenseMember> {
        val routingMembers = membership.getCurrentTransportRoutingMembers(groupId).getOrThrow().orEmpty()
            .associateBy { it.contactId }
        val localName = getLocalIdentityName().getOrNull().orEmpty()
        return buildList {
            localMemberId?.let { add(ExpenseMember(it, localName, true)) }
            contactIds.sorted().forEach { contactId ->
                val signingKey = routingMembers[contactId]?.signingPublicKey ?: return@forEach
                val name = contactNames[contactId]?.takeIf(String::isNotBlank) ?: contactId
                val id = ExpenseMemberId.fromSigningPublicKey(signingKey)
                if (id != localMemberId && none { it.id == id }) add(ExpenseMember(id, name, false, contactId))
            }
        }
    }
}
