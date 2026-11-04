package com.cbgm.sparrow.feature.membership.domain.model

import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket

data class OpenedGroupWelcomeDto(
    val packet: GroupCreatedPacket,
    val groupKey: ByteArray,
    /** The installed epoch is another admin's version of this epoch; this one wins and replaces it. */
    val replacesContestedEpoch: Boolean = false,
    /** The replaced version was this device's own change, which is therefore dropped. */
    val dropsLocalChange: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenedGroupWelcomeDto) return false

        if (packet != other.packet) return false
        if (!groupKey.contentEquals(other.groupKey)) return false
        if (replacesContestedEpoch != other.replacesContestedEpoch) return false
        if (dropsLocalChange != other.dropsLocalChange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packet.hashCode()
        result = 31 * result + groupKey.contentHashCode()
        result = 31 * result + replacesContestedEpoch.hashCode()
        result = 31 * result + dropsLocalChange.hashCode()
        return result
    }
}

data class SecuredGroupMessageDto(
    val epoch: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val senderSignature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecuredGroupMessageDto) return false

        if (epoch != other.epoch) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!senderSignature.contentEquals(other.senderSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = epoch
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + senderSignature.contentHashCode()
        return result
    }
}
