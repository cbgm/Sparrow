package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase

/** An invitation supplies a genuine contact phone once; membership then retains it per epoch. */
class RequireMemberPhoneNumberUseCase(
    private val getContact: GetContactUseCase
) {
    suspend operator fun invoke(contactId: String): Result<String> =
        runCatching {
            getContact(contactId).getOrThrow()?.preferredPhoneNumber?.value
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: error("Accepted group member has no real phone number")
        }
}
