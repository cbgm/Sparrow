package com.cbgm.sparrow.feature.contacts.domain.model

import com.cbgm.sparrow.core.crypto.safety.SafetyNumber

data class ContactDetailsContext(
    val contact: Contact?,
    val safetyNumber: SafetyNumber?,
    val profilePictureBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactDetailsContext

        if (contact != other.contact) return false
        if (safetyNumber != other.safetyNumber) return false
        if (!profilePictureBytes.contentEquals(other.profilePictureBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contact?.hashCode() ?: 0
        result = 31 * result + (safetyNumber?.hashCode() ?: 0)
        result = 31 * result + (profilePictureBytes?.contentHashCode() ?: 0)
        return result
    }
}
