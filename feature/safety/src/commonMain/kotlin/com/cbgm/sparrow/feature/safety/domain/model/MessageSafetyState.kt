package com.cbgm.sparrow.feature.safety.domain.model

sealed interface MessageSafetyState {
    data object Disabled : MessageSafetyState

    data object Preparing : MessageSafetyState

    data object Analyzing : MessageSafetyState

    data object Ready : MessageSafetyState

    data class Failed(
        val message: String
    ) : MessageSafetyState
}
