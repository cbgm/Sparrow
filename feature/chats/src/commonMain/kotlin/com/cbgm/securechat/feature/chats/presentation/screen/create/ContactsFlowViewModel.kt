package com.cbgm.securechat.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.usecase.GetOrCreateDirectConversation
import com.cbgm.securechat.feature.chats.presentation.model.ContactsFlowUiEvent
import kotlinx.coroutines.launch

class ContactsFlowViewModel(
    private val getOrCreateDirectConversation: GetOrCreateDirectConversation
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
