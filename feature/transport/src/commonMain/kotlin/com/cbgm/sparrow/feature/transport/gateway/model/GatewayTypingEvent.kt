package com.cbgm.sparrow.feature.transport.gateway.model

data class GatewayTypingEvent(
    val senderId: String,
    val isTyping: Boolean
)
