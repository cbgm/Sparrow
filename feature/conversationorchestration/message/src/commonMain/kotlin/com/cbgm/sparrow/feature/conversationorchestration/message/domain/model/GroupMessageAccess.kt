package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

sealed interface GroupMessageAccess {
    data object Granted : GroupMessageAccess

    data class Denied(
        val reason: String
    ) : GroupMessageAccess
}
