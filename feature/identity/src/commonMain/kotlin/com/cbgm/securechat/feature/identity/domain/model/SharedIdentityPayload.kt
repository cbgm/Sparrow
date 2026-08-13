package com.cbgm.securechat.feature.identity.domain.model

/**
 * Portable SecureChat identity shared through QR code, the platform
 * share sheet, text, or a future deep link.
 *
 * Public keys and the approved local phone number are always included.
 * The display name is optional.
 */
data class SharedIdentityPayload(
    val version: Int,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val contactDetails: SharedContactDetails
) {
    init {
        require(version > 0) {
            "Identity payload version must be positive"
        }

        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }

        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        require(contactDetails.phoneNumber.isNotBlank()) {
            "Shared identity phone number must not be blank"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharedIdentityPayload) return false

        return version == other.version &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            contactDetails == other.contactDetails
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + contactDetails.hashCode()
        return result
    }
}

/**
 * Stable contact information attached to every shared identity.
 *
 * The phone number is mandatory because it anchors contact matching,
 * chat history, and routing. The display name is optional.
 */
data class SharedContactDetails(
    val displayName: String?,
    val phoneNumber: String
) {
    init {
        require(phoneNumber.isNotBlank()) {
            "Shared phone number must not be blank"
        }
    }
}
