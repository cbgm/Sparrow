package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

sealed interface ForwardingSelectionUiState {
    val searchQuery: String

    data class Loading(
        override val searchQuery: String = ""
    ) : ForwardingSelectionUiState

    data class Empty(
        override val searchQuery: String = ""
    ) : ForwardingSelectionUiState

    data class Content(
        val chats: List<ForwardingTargetUi>,
        val contacts: List<ForwardingTargetUi>,
        override val searchQuery: String = ""
    ) : ForwardingSelectionUiState
}
