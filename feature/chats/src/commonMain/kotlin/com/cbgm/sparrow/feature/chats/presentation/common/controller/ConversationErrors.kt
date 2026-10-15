package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationErrors(
    tag: String
) {
    private val logger = SparrowLog.withTag(tag)
    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages.asStateFlow()

    fun report(message: String) {
        logger.error { message }
        _messages.value = message
    }

    fun clear() {
        _messages.value = null
    }
}
