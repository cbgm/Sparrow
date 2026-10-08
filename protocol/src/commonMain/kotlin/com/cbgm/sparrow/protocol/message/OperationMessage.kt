package com.cbgm.sparrow.protocol.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OperationMessage(
    val operation: MessageOperation
)

@Serializable
sealed interface MessageOperation {
    val messageId: String

    @Serializable
    @SerialName("EDIT")
    data class Edit(
        override val messageId: String,
        val text: String,
        val editedAtEpochMilliseconds: Long
    ) : MessageOperation {
        init {
            require(messageId.isNotBlank()) { "Edited message ID must not be blank" }
            require(text.isNotBlank()) { "Edited message text must not be blank" }
            require(editedAtEpochMilliseconds >= 0L) { "Edit timestamp must not be negative" }
        }
    }

    @Serializable
    @SerialName("DELETE")
    data class Delete(
        override val messageId: String,
        val deletedAtEpochMilliseconds: Long
    ) : MessageOperation {
        init {
            require(messageId.isNotBlank()) { "Deleted message ID must not be blank" }
            require(deletedAtEpochMilliseconds >= 0L) { "Deletion timestamp must not be negative" }
        }
    }

    @Serializable
    @SerialName("REACTION")
    data class Reaction(
        override val messageId: String,
        val emoji: String,
        val removed: Boolean = false
    ) : MessageOperation {
        init {
            require(messageId.isNotBlank()) { "Reaction target message ID must not be blank" }
            require(emoji.isNotBlank()) { "Reaction emoji must not be blank" }
        }
    }

    @Serializable
    @SerialName("POLL_VOTE")
    data class PollVote(
        override val messageId: String,
        val pollId: String,
        val selectedOptionIds: Set<String>
    ) : MessageOperation {
        init {
            require(messageId.isNotBlank()) { "Poll target message ID must not be blank" }
            require(pollId.isNotBlank()) { "Poll ID must not be blank" }
            require(selectedOptionIds.isNotEmpty()) { "Poll vote requires at least one option" }
            require(selectedOptionIds.none(String::isBlank)) { "Poll option ID must not be blank" }
        }
    }

    @Serializable
    @SerialName("POLL_CLOSE")
    data class PollClose(
        override val messageId: String,
        val pollId: String,
        val closedAtEpochMilliseconds: Long
    ) : MessageOperation {
        init {
            require(messageId.isNotBlank()) { "Poll target message ID must not be blank" }
            require(pollId.isNotBlank()) { "Poll ID must not be blank" }
            require(closedAtEpochMilliseconds > 0L) { "Poll close timestamp must be positive" }
        }
    }
}

class OperationMessageCodec(
    private val json: Json
) {
    fun encode(message: OperationMessage): String =
        FORMAT_PREFIX + json.encodeToString(message)

    fun canDecode(plaintext: String): Boolean = plaintext.startsWith(FORMAT_PREFIX)

    fun decode(plaintext: String): OperationMessage {
        require(canDecode(plaintext)) { "Plaintext is not an operation message" }
        return json.decodeFromString(plaintext.removePrefix(FORMAT_PREFIX))
    }

    private companion object {
        const val FORMAT_PREFIX = "sparrow-operation-message-v1:"
    }
}
