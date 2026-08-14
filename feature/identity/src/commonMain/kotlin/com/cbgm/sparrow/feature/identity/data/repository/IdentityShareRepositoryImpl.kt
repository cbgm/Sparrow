package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.extensions.escapeShareValue
import com.cbgm.sparrow.core.extensions.unescapeShareValue
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository

/**
 * Text codec for shared Sparrow identities.
 *
 * Version 1 format:
 *
 * sc1|ek=<hex>|sk=<hex>|phone=<escaped>|name=<escaped>
 *
 * The phone field is mandatory. The name field is optional.
 */
class IdentityShareRepositoryImpl : IdentityShareRepository {
    override fun encode(payload: SharedIdentityPayload): Result<String> =
        runCatching {
            require(payload.version == SUPPORTED_VERSION) {
                "Unsupported identity payload version: ${payload.version}"
            }

            require(payload.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(payload.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val phoneNumber =
                payload.contactDetails.phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error("Shared identity phone number is missing")

            buildList {
                add(FORMAT_PREFIX)
                add("$ENCRYPTION_KEY_FIELD=${payload.encryptionPublicKey.toHexString()}")
                add("$SIGNING_KEY_FIELD=${payload.signingPublicKey.toHexString()}")
                add("$PHONE_NUMBER_FIELD=${phoneNumber.escapeShareValue()}")
                payload.contactDetails.displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { displayName ->
                        add("$DISPLAY_NAME_FIELD=${displayName.escapeShareValue()}")
                    }
            }.joinToString(separator = FIELD_SEPARATOR)
        }

    override fun decode(encodedValue: String): Result<SharedIdentityPayload> =
        runCatching {
            require(encodedValue.isNotBlank()) {
                "Shared identity payload is empty"
            }

            val parts = encodedValue.split(FIELD_SEPARATOR)

            require(parts.firstOrNull() == FORMAT_PREFIX) {
                "This is not a supported Sparrow identity payload"
            }

            val values =
                parts
                    .drop(1)
                    .associate { part ->
                        val separatorIndex = part.indexOf(KEY_VALUE_SEPARATOR)

                        require(separatorIndex > 0) {
                            "Malformed identity payload field"
                        }

                        part.substring(
                            startIndex = 0,
                            endIndex = separatorIndex
                        ) to
                            part.substring(
                                startIndex = separatorIndex + 1
                            )
                    }

            val encryptionPublicKey =
                values[ENCRYPTION_KEY_FIELD]?.hexToByteArray()
                    ?: error("Encryption public key is missing")

            val signingPublicKey =
                values[SIGNING_KEY_FIELD]?.hexToByteArray()
                    ?: error("Signing public key is missing")

            val phoneNumber =
                values[PHONE_NUMBER_FIELD]?.unescapeShareValue()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: error("Shared identity phone number is missing")

            val displayName =
                values[DISPLAY_NAME_FIELD]?.unescapeShareValue()?.trim()?.takeIf { it.isNotEmpty() }

            SharedIdentityPayload(
                version = SUPPORTED_VERSION,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                contactDetails =
                    SharedContactDetails(
                        displayName = displayName,
                        phoneNumber = phoneNumber
                    )
            )
        }

    private companion object {
        const val SUPPORTED_VERSION = 1
        const val FORMAT_PREFIX = "sc1"
        const val FIELD_SEPARATOR = "|"
        const val KEY_VALUE_SEPARATOR = "="
        const val ENCRYPTION_KEY_FIELD = "ek"
        const val SIGNING_KEY_FIELD = "sk"
        const val DISPLAY_NAME_FIELD = "name"
        const val PHONE_NUMBER_FIELD = "phone"
    }
}
