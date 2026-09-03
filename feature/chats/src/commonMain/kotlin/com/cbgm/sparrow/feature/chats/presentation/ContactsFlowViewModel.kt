package com.cbgm.sparrow.feature.chats.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.GetOrCreateDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.presentation.create.model.ContactsFlowUiEvent
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ContactsFlowViewModel(
    private val getOrCreateDirectConversation: GetOrCreateDirectConversationUseCase,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val observeIdentitySetupMode: ObserveIdentitySetupModeUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase
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
            when (observeIdentitySetupMode().first()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                    openAutomaticContact(contactId, contactName)

                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                    openManualContact(contactId, contactName)
            }
        }
    }

    private suspend fun openAutomaticContact(
        contactId: String,
        contactName: String
    ) {
        if (requireDirectChatAuthorization(contactId).isSuccess) {
            openDirectConversation(contactId, contactName)
            return
        }

        ensureIdentityExchangeStarted(contactId)
    }

    private suspend fun openManualContact(
        contactId: String,
        contactName: String
    ) {
        getOrCreateDirectConversation(contactId).onSuccess { conversationId ->
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

    private suspend fun openDirectConversation(
        contactId: String,
        contactName: String
    ) {
        getOrCreateDirectConversation(contactId).onSuccess { conversationId ->
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
}
