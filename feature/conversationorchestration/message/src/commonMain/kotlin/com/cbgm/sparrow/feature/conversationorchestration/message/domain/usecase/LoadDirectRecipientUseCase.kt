package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectRecipient

/** Resolves the contact a direct conversation belongs to and the name its attachments are filed under. */
class LoadDirectRecipientUseCase(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(target: DirectConversationTarget): Result<DirectRecipient> =
        safeSuspendCall(logFailure = false) {
            val contact = contactRepository.getContact(target.contactId).getOrThrow() ?: error("Contact was not found")
            DirectRecipient(
                target = target,
                displayName = contact.displayName?.takeIf(String::isNotBlank)
                    ?: contact.preferredPhoneNumber?.value
                    ?: target.contactId
            )
        }
}
