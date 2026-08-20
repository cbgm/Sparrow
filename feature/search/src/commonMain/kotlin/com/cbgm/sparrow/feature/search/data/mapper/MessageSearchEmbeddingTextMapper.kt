package com.cbgm.sparrow.feature.search.data.mapper

import com.cbgm.sparrow.data.database.model.MessageSearchSource

internal fun MessageSearchSource.toEmbeddingText(): String =
    buildList {
        senderName.cleanOrNull()?.let { add("Sender: $it") }
        conversationDisplayName()?.let { add("Conversation: $it") }
        add("Message: ${text.trim()}")
    }.joinToString(separator = "\n")

private fun MessageSearchSource.conversationDisplayName(): String? =
    conversationTitle.cleanOrNull() ?: contactName.cleanOrNull()

private fun String?.cleanOrNull(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
