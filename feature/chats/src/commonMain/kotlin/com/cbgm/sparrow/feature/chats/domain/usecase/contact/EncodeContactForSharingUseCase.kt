package com.cbgm.sparrow.feature.chats.domain.usecase.contact

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

class EncodeContactForSharingUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(contact: Contact): Result<String?> = invoke(
        contactId = contact.id,
        displayName = contact.displayName,
        phoneNumber = contact.preferredPhoneNumber?.value
    )

    suspend operator fun invoke(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<String?> {
        val identity = getRemoteIdentity(contactId).getOrElse { return Result.failure(it) }
            ?: return Result.success(null)
        val validatedPhoneNumber = phoneNumber?.takeIf(String::isNotBlank)
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
                                displayName = displayName,
                                phoneNumber = validatedPhoneNumber
                            )
                    )
            ).map { encoded -> encoded }
    }
}
