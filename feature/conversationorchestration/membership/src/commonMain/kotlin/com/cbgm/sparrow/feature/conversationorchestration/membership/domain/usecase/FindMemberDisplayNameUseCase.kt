package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase

/** The contact's name for group history events; unknown or unnamed contacts read as "Member". */
class FindMemberDisplayNameUseCase(
    private val getContact: GetContactUseCase
) {
    suspend operator fun invoke(contactId: String): Result<String> =
        runCatching {
            getContact(contactId).getOrThrow()?.displayName?.trim()?.takeIf(String::isNotEmpty) ?: UNKNOWN_MEMBER_NAME
        }

    private companion object {
        const val UNKNOWN_MEMBER_NAME = "Member"
    }
}
