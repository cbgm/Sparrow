package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactDetailsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveContactDetailsContextUseCase(
    private val observeContact: ObserveContactUseCase,
    private val getContactSafetyNumber: GetContactSafetyNumberUseCase
) {
    operator fun invoke(contactId: String): Flow<ContactDetailsContext> =
        observeContact(contactId).map { contact ->
            val safetyNumber =
                if (contact?.sparrowIdentity == null) {
                    null
                } else {
                    getContactSafetyNumber(contactId).getOrThrow()
                }

            ContactDetailsContext(
                contact = contact,
                safetyNumber = safetyNumber
            )
        }
}
