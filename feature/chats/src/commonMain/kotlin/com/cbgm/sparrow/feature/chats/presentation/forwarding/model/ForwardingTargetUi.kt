package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget

data class ForwardingTargetUi(
    val id: String,
    val displayName: String,
    val avatarBytes: ByteArray?,
    val target: ForwardingTarget
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ForwardingTargetUi

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (!avatarBytes.contentEquals(other.avatarBytes)) return false
        if (target != other.target) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + target.hashCode()
        return result
    }
}
