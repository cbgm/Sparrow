package com.cbgm.sparrow.feature.chats.presentation.forwarding.model

sealed interface ForwardingSelectionUiEvent {
    data class SearchQueryChanged(
        val query: String
    ) : ForwardingSelectionUiEvent

    data class TargetClicked(
        val target: ForwardingTargetUi
    ) : ForwardingSelectionUiEvent
}
