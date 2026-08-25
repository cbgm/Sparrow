package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactDetailsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ObserveContactDetailsContextUseCase(
    private val observeContact: ObserveContactUseCase,
    private val observeProfilePicture: ObserveContactProfilePictureUseCase,
    private val getContactSafetyNumber: GetContactSafetyNumberUseCase
) {
    operator fun invoke(contactId: String): Flow<ContactDetailsContext> =
        combine(
            observeContact(contactId),
            observeProfilePicture(contactId)
        ) { contact, profilePictureBytes ->
            ContactSnapshot(
                contact = contact,
                profilePictureBytes = profilePictureBytes
            )
        }.map { snapshot ->
            val contact = snapshot.contact
            val safetyNumber =
                if (contact?.sparrowIdentity == null) {
                    null
                } else {
                    getContactSafetyNumber(contactId).getOrThrow()
                }

            ContactDetailsContext(
                contact = contact,
                safetyNumber = safetyNumber,
                profilePictureBytes = snapshot.profilePictureBytes
            )
        }

    private data class ContactSnapshot(
        val contact: Contact?,
        val profilePictureBytes: ByteArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ContactSnapshot

            if (contact != other.contact) return false
            if (!profilePictureBytes.contentEquals(other.profilePictureBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contact?.hashCode() ?: 0
            result = 31 * result + (profilePictureBytes?.contentHashCode() ?: 0)
            return result
        }
    }
}
