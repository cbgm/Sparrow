package com.cbgm.sparrow.feature.chats.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.presentation.create.model.ContactsFlowUiEvent
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureException
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_contact_blocked
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

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
                }.onFailure { error ->
                    showBlockedContact(error)
                }
        }
    }

    /** A blocked contact is not an error: the user is told and can unblock it right away. */
    private suspend fun showBlockedContact(error: Throwable) {
        if ((error as? ConversationFailureException)?.failure?.reason != ConversationFailureReason.CONTACT_BLOCKED) return
        SparrowLog.hint(getString(Res.string.feature_chats_contact_blocked))
        navigator.navigateTo(AppRoute.BlockedContacts)
    }

    private fun openImportContact() {
        navigator.navigateTo(AppRoute.ImportContact())
    }
}
