package com.cbgm.sparrow.feature.chats.domain.usecase.contact

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository

class EncodeContactForSharingUseCase(
    private val identityShareRepository: IdentityShareRepository
) {
    operator fun invoke(contact: Contact): Result<String?> {
        val identity = contact.sparrowIdentity ?: return Result.success(null)
        val phoneNumber =
            contact
                .preferredPhoneNumber
                ?.value
                ?.takeIf(String::isNotBlank)
                ?: return Result.success(null)

        return identityShareRepository
            .encode(
                payload =
                    SharedIdentityPayload(
                        version = 1,
                        encryptionPublicKey = identity.encryptionPublicKey,
                        signingPublicKey = identity.signingPublicKey,
                        contactDetails =
                            SharedContactDetails(
                                displayName = contact.displayName,
                                phoneNumber = phoneNumber
                            )
                    )
            ).map { encoded -> encoded }
    }
}
