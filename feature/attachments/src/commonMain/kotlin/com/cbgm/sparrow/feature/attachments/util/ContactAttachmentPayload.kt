package com.cbgm.sparrow.feature.attachments.util

import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact

object ContactAttachmentPayload {
    fun encode(contact: SharedContact): ByteArray =
        buildString {
            append(contact.phoneNumber)
            append(SEPARATOR)
            append(contact.displayName.orEmpty())
        }.encodeToByteArray()

    fun decode(bytes: ByteArray): SharedContact? {
        val value = runCatching { bytes.decodeToString() }.getOrNull() ?: return null
        val separatorIndex = value.indexOf(SEPARATOR)
        if (separatorIndex < 0) return null

        val phoneNumber = value.substring(0, separatorIndex).trim()
        if (phoneNumber.isBlank()) return null

        val displayName =
            value
                .substring(separatorIndex + 1)
                .trim()
                .takeIf(String::isNotBlank)

        return runCatching {
            SharedContact(
                displayName = displayName,
                phoneNumber = phoneNumber
            )
        }.getOrNull()
    }

    private const val SEPARATOR = '\n'
}
