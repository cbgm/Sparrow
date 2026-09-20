package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Prepares only verified owner-provided inputs; Chats persists its own verification rows. */
class GroupVerificationInputsUseCase(
    private val membershipRepository: GroupMembershipRepository,
    private val contacts: ContactRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend fun membershipContext(groupId: String): GroupVerificationMembershipContext =
        membershipRepository.getVerificationMembershipContext(groupId).getOrThrow()

    /**
     * Resolve the membership snapshot and its Contacts/Identity projections together.
     * Chats receives plain inputs and is responsible only for its verification records.
     * Never load Identity keys for a pending member that is already active in this epoch.
     */
    suspend fun ownedState(groupId: String): OwnedGroupVerificationInputs {
        val membership = membershipContext(groupId)
        val activeContactIds = membership.memberKeys.mapTo(mutableSetOf()) { it.contactId }
        val pendingContactIds = membership.memberships.asSequence()
            .filter { it.isOwner && it.isVisiblePending && it.contactId !in activeContactIds }
            .map { it.contactId }
            .distinct()
            .toList()
        val contactIds = (activeContactIds + pendingContactIds).toList()
        val displayNames = contactIds.associateWith { contactId ->
            val contact = contacts.getContact(contactId).getOrThrow()
                ?: error("Contact not found: $contactId")
            contact.displayName?.trim()?.takeIf(String::isNotBlank) ?: "Unknown member"
        }
        val pendingIdentities = pendingContactIds.associateWith { contactId ->
            getRemoteIdentity(contactId).getOrThrow()?.let { identity ->
                PendingGroupIdentity(
                    encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                    signingPublicKey = identity.signingPublicKey.copyOf()
                )
            }
        }
        return OwnedGroupVerificationInputs(
            membership = membership,
            displayNames = displayNames,
            pendingIdentities = pendingIdentities
        )
    }
}

/** Group verification input snapshot. The keys are copied from Identity and are never DAO entities. */
class OwnedGroupVerificationInputs(
    val membership: GroupVerificationMembershipContext,
    val displayNames: Map<String, String>,
    val pendingIdentities: Map<String, PendingGroupIdentity?>
) {
    fun requireDisplayName(contactId: String): String =
        displayNames[contactId] ?: error("Group verification contact was not resolved: $contactId")
}

/** A copy of the Identity-owned pending member keys; no live DB entity is exposed. */
class PendingGroupIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray
)
