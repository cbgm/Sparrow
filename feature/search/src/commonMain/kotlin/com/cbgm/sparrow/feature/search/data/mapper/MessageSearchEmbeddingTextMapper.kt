package com.cbgm.sparrow.feature.search.data.mapper

import com.cbgm.sparrow.data.database.model.MessageSearchSourceDto

internal fun MessageSearchSourceDto.toEmbeddingText(): String =
    buildList {
        senderName.cleanOrNull()?.let { add("Sender: $it") }
        messageSearchConversationName(conversationTitle, contactName)?.let { add("Conversation: $it") }
        add("Message: ${text.trim()}")
    }.joinToString(separator = "\n")

internal fun messageSearchConversationName(
    conversationTitle: String?,
    contactName: String?
): String? = conversationTitle.cleanOrNull() ?: contactName.cleanOrNull()

private fun String?.cleanOrNull(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
