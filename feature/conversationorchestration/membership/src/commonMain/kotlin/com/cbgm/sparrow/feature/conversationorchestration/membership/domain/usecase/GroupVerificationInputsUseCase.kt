package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.OwnedGroupVerificationInputs
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.PendingGroupIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Loads Membership, Contacts and Identity inputs of the group verification projection. */
class GroupVerificationInputsUseCase(
    private val membershipRepository: GroupMembershipRepository,
    private val contacts: ContactRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend fun membershipContext(groupId: String): Result<GroupVerificationMembershipContext> =
        membershipRepository.getVerificationMembershipContext(groupId)

    /**
     * Resolve the membership snapshot and its Contacts/Identity projections together.
     * Never load Identity keys for a pending member that is already active in this epoch.
     */
    suspend fun ownedState(groupId: String): Result<OwnedGroupVerificationInputs> =
        safeSuspendCall(logFailure = false) {
            val membership = membershipContext(groupId).getOrThrow()
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
            OwnedGroupVerificationInputs(
                membership = membership,
                displayNames = displayNames,
                pendingIdentities = pendingIdentities
            )
        }
}
