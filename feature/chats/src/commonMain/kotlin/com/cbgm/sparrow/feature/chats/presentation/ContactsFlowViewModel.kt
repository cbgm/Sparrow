package com.cbgm.sparrow.feature.chats.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.presentation.create.model.ContactsFlowUiEvent
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import kotlinx.coroutines.launch

class ContactsFlowViewModel(
    private val getOrCreateDirectConversation: GetOrCreateDirectConversationUseCase,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase
) : BaseViewModel() {
    fun onUiEvent(event: ContactsFlowUiEvent) {
        when (event) {
            is ContactsFlowUiEvent.ContactSelected ->
                openContact(
                    contactId = event.contactId,
                    contactName = event.contactName
                )
            ContactsFlowUiEvent.ImportContactClicked -> openImportContact()
            is ContactsFlowUiEvent.GroupCreated -> openGroup(event.conversationId)
        }
    }

    private fun openContact(
        contactId: String,
        contactName: String
    ) {
        viewModelScope.launch {
            val conversationId = getOrCreateDirectConversation(contactId)
            ensureIdentityExchangeStarted(contactId)
            navigator.navigateTo(
                AppRoute.Chat(
                    conversationId = conversationId,
                    contactId = contactId,
                    contactName = contactName
                )
            )
        }
    }

    private fun openImportContact() {
        navigator.navigateTo(AppRoute.ImportContact())
    }

    private fun openGroup(conversationId: String) {
        navigator.navigateTo(AppRoute.GroupConversation(conversationId = conversationId))
    }
}
