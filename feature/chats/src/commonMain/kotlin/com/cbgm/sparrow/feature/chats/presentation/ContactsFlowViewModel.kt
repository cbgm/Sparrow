package com.cbgm.sparrow.feature.chats.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.presentation.create.model.ContactsFlowUiEvent
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import kotlinx.coroutines.launch

class ContactsFlowViewModel(
    private val orchestrator: ConversationOrchestrator
) : BaseViewModel() {
    fun onUiEvent(event: ContactsFlowUiEvent) {
        when (event) {
            is ContactsFlowUiEvent.ContactSelected ->
                openContact(
                    contactId = event.contactId,
                    contactName = event.contactName
                )

            ContactsFlowUiEvent.ImportContactClicked -> openImportContact()
        }
    }

    private fun openContact(
        contactId: String,
        contactName: String
    ) {
        viewModelScope.launch {
            orchestrator.awaitResult(DirectConversationCommand.OpenConversation(contactId))
                .onSuccess { result ->
                    val conversationId = (result as? ConversationResult.ConversationOpened)?.conversationId
                    if (conversationId != null) {
                        navigator.navigateTo(
                            AppRoute.Chat(
                                conversationId = conversationId,
                                contactId = contactId,
                                contactName = contactName
                            )
                        )
                    }
                }
        }
    }

    private fun openImportContact() {
        navigator.navigateTo(AppRoute.ImportContact())
    }
}
