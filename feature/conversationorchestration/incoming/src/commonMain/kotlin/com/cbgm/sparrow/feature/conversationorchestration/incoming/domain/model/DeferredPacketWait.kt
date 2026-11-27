package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

/**
 * What a packet that arrived too early waits for. [key] is stored with its parked envelope; all envelopes
 * with the same key are replayed together once it is met.
 */
sealed interface DeferredPacketWait {
    val key: String

    /** The group's welcome for [epoch] (or a later one) is installed. */
    data class GroupEpoch(
        val groupId: String,
        val epoch: Int
    ) : DeferredPacketWait {
        override val key: String = "$GROUP_EPOCH_PREFIX$groupId:$epoch"
    }

    /** The identity exchange with [contactId] is no longer pending. */
    data class ContactExchange(
        val contactId: String
    ) : DeferredPacketWait {
        override val key: String = "$CONTACT_EXCHANGE_PREFIX$contactId"
    }

    companion object {
        private const val GROUP_EPOCH_PREFIX = "group-epoch:"
        private const val CONTACT_EXCHANGE_PREFIX = "contact-exchange:"

        /** The wait a stored key stands for, or null for a key this build does not know. */
        fun fromKey(key: String): DeferredPacketWait? =
            when {
                key.startsWith(GROUP_EPOCH_PREFIX) -> groupEpoch(key.removePrefix(GROUP_EPOCH_PREFIX))
                key.startsWith(CONTACT_EXCHANGE_PREFIX) ->
                    key.removePrefix(CONTACT_EXCHANGE_PREFIX).takeIf(String::isNotBlank)?.let(::ContactExchange)
                else -> null
            }

        private fun groupEpoch(value: String): GroupEpoch? {
            val groupId = value.substringBeforeLast(':', missingDelimiterValue = "")
            val epoch = value.substringAfterLast(':').toIntOrNull()
            return if (groupId.isBlank() || epoch == null) null else GroupEpoch(groupId, epoch)
        }
    }
}
