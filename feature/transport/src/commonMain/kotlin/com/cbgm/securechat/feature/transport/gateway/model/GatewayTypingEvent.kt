package com.cbgm.securechat.feature.transport.gateway.model

data class GatewayTypingEvent(
    val senderId: String,
    val isTyping: Boolean
)
