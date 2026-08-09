package com.cbgm.securechat.feature.chats.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicator
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ChatViewModel(
    private val conversationId: String,
    private val contactId: String,
    private val fallbackContactName: String,
    private val observeConversation: ObserveConversation,
    private val sendMessageUseCase: SendMessage,
    private val markConversationReadUseCase: MarkConversationRead,
    private val retryFailedMessage: RetryMessage,
    private val directIdentitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val identityExchangeStarter: IdentityExchangeStarter,
    identityInvitationService: IdentityInvitationService,
    observeContact: ObserveContact,
    private val observeTypingIndicator: ObserveTypingIndicator,
    private val setTypingIndicator: SetTypingIndicator
) : ViewModel() {
    private val logger = SecureChatLog.withTag("ChatViewModel")

    private val messageText = MutableStateFlow("")

    private val errorMessage = MutableStateFlow<String?>(null)

    private val isContactTyping = MutableStateFlow(false)

    private var localTypingStopJob: Job? = null

    private var remoteTypingTimeoutJob: Job? = null

    private var isLocalTyping = false

    private val contactFlow: Flow<Contact?> =
        observeContact(contactId = contactId)

    private val conversationFlow: Flow<Conversation?> =
        observeConversation(conversationId)

    private val identityHandshakeStateFlow: Flow<IdentityHandshakeState?> =
        identityInvitationService.observeState(contactId)

    private val directIdentitySetupModeFlow: StateFlow<DirectIdentitySetupMode> =
        directIdentitySetupModeRepository
            .observeMode()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING
            )

    private val chatContentFlow: Flow<ChatContentState> =
        combine(
            conversationFlow,
            contactFlow,
            identityHandshakeStateFlow,
            directIdentitySetupModeFlow
        ) { conversation, contact, identityHandshakeState, directIdentitySetupMode ->
            ChatContentState(
                conversation = conversation,
                contact = contact,
                identityHandshakeState = identityHandshakeState,
                directIdentitySetupMode = directIdentitySetupMode
            )
        }

    private val composerFlow: Flow<ComposerState> =
        combine(
            messageText,
            errorMessage,
            isContactTyping
        ) { currentMessageText, currentError, contactTyping ->

            ComposerState(
                messageText = currentMessageText,
                errorMessage = currentError,
                isContactTyping = contactTyping
            )
        }

    private val screenContentFlow: Flow<ScreenContentState> =
        combine(
            chatContentFlow,
            composerFlow
        ) { chatContent, composer ->

            ScreenContentState(
                chatContent = chatContent,
                composer = composer
            )
        }

    val uiState: StateFlow<ChatUiState> =
        screenContentFlow
            .map { screenContent ->
                val conversation = screenContent.chatContent.conversation

                val contact = screenContent.chatContent.contact

                val composer = screenContent.composer

                ChatUiState(
                    contactId = contactId,
                    contactName =
                        resolveContactName(
                            contact = contact,
                            conversation = conversation
                        ),
                    messages = conversation?.messages?.reversed().orEmpty(),
                    messageText = composer.messageText,
                    isContactTyping = composer.isContactTyping,
                    contactSecurityState = contact.toSecurityState(),
                    identityHandshakeState = screenContent.chatContent.identityHandshakeState,
                    directIdentitySetupMode = screenContent.chatContent.directIdentitySetupMode,
                    isLoadingContact = contact == null,
                    isMessageInputEnabled =
                        isDirectChatAuthorized(
                            contact = contact,
                            identityHandshakeState = screenContent.chatContent.identityHandshakeState,
                            directIdentitySetupMode = screenContent.chatContent.directIdentitySetupMode
                        ),
                    errorMessage = composer.errorMessage
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue =
                    ChatUiState(
                        contactId = contactId,
                        contactName = fallbackContactName,
                        isMessageInputEnabled = false
                    )
            )

    init {
        observeIdentitySetupMode()
        observeIncomingTypingEvents()
    }

    private fun observeIdentitySetupMode() {
        viewModelScope.launch {
            directIdentitySetupModeFlow
                .collect { mode ->
                    if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                        identityExchangeStarter
                            .ensureStarted(contactId)
                            .onFailure { error ->
                                errorMessage.value = error.message ?: "Contact invitation could not be started"
                            }
                    }
                }
        }
    }

    fun onMessageTextChanged(value: String) {
        if (!uiState.value.isMessageInputEnabled) {
            return
        }

        messageText.value = value
        errorMessage.value = null

        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (value.isBlank()) {
            stopTyping()
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            sendTypingState(isTyping = true)
        }

        localTypingStopJob =
            viewModelScope.launch {
                delay(LOCAL_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                stopTypingNow()
            }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    fun sendMessage() {
        if (!uiState.value.isMessageInputEnabled) {
            return
        }

        val normalizedText = messageText.value.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        messageText.value = ""
        errorMessage.value = null
        stopTyping()

        viewModelScope.launch {
            runCatching {
                sendMessageUseCase(
                    conversationId = conversationId,
                    text = normalizedText
                )
            }.onFailure { error ->
                messageText.value = normalizedText
                errorMessage.value = error.message ?: "Message could not be sent"
            }
        }
    }

    fun retryMessage(messageId: String) {
        if (messageId.isBlank()) {
            return
        }

        errorMessage.value = null

        viewModelScope.launch {
            retryFailedMessage(messageId)
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued again"
                }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationReadUseCase(conversationId)
                .onFailure { error ->
                    logger.warn(error) { "Could not mark conversation as read" }
                }
        }
    }

    private fun observeIncomingTypingEvents() {
        viewModelScope.launch {
            observeTypingIndicator(contactId = contactId)
                .collect { isTyping ->
                    remoteTypingTimeoutJob?.cancel()
                    isContactTyping.value = isTyping

                    if (isTyping) {
                        remoteTypingTimeoutJob =
                            viewModelScope.launch {
                                delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                                isContactTyping.value = false
                            }
                    }
                }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch {
            sendTypingStateNow(isTyping = isTyping)
        }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        setTypingIndicator(
            contactId = contactId,
            isTyping = isTyping
        ).onFailure { error ->
            logger.warn(error) { "Could not send typing state for $contactId" }
        }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private fun resolveContactName(
        contact: Contact?,
        conversation: Conversation?
    ): String =
        contact
            ?.displayName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: conversation
                ?.contactName
                ?.takeIf {
                    it.isNotBlank()
                }
            ?: fallbackContactName
                .takeIf {
                    it.isNotBlank()
                }
            ?: "Unknown contact"

    private fun Contact?.toSecurityState(): ContactSecurityState {
        val identity = this?.secureChatIdentity ?: return ContactSecurityState.NO_REMOTE_PUBLIC_KEYS

        if (identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            return ContactSecurityState.ONE_WAY_KEYS
        }

        val verifiedByMe = identity.verificationStatus == ContactVerificationStatus.VERIFIED
        val verifiedByContact = identity.verifiedByContact

        return when {
            verifiedByMe && verifiedByContact -> ContactSecurityState.MUTUAL_KEYS_VERIFIED
            verifiedByMe -> ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME
            verifiedByContact -> ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT
            else -> ContactSecurityState.MUTUAL_KEYS_UNVERIFIED
        }
    }

    private fun isDirectChatAuthorized(
        contact: Contact?,
        identityHandshakeState: IdentityHandshakeState?,
        directIdentitySetupMode: DirectIdentitySetupMode
    ): Boolean =
        when (directIdentitySetupMode) {
            DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                identityHandshakeState == IdentityHandshakeState.MUTUAL_UNVERIFIED

            DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                contact?.secureChatIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL
        }

    private data class ChatContentState(
        val conversation: Conversation?,
        val contact: Contact?,
        val identityHandshakeState: IdentityHandshakeState?,
        val directIdentitySetupMode: DirectIdentitySetupMode
    )

    private data class ComposerState(
        val messageText: String,
        val errorMessage: String?,
        val isContactTyping: Boolean
    )

    private data class ScreenContentState(
        val chatContent: ChatContentState,
        val composer: ComposerState
    )

    private companion object {
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
