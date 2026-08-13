package com.cbgm.securechat.feature.contactimport.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec

/**
 * Decodes and imports a shared SecureChat identity.
 *
 * A phone number is mandatory because it is the stable contact,
 * conversation, and relay-routing anchor. The contact repository then
 * merges by normalized phone number before considering public keys.
 */
class ImportSharedIdentityUseCase(
    private val identityShareCodec: IdentityShareCodec,
    private val importContact: ImportContact
) {
    suspend operator fun invoke(
        encodedIdentity: String,
        contactId: String? = null,
        identityImportTrust: IdentityImportTrust = IdentityImportTrust.UNVERIFIED
    ): Result<Contact> =
        runCatching {
            val sharedIdentity = identityShareCodec.decode(encodedIdentity).getOrThrow()

            val phoneNumber =
                sharedIdentity
                    .contactDetails
                    .phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error(
                        "Shared identity does not contain a phone number"
                    )

            importContact(
                request =
                    ImportContactRequest(
                        contactId = contactId,
                        encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                        displayName = sharedIdentity.contactDetails.displayName,
                        phoneNumber = phoneNumber,
                        identityImportTrust = identityImportTrust
                    )
            ).getOrThrow()
        }
}
