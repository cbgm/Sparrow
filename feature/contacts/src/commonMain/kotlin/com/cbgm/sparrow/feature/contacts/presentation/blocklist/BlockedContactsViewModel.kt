package com.cbgm.sparrow.feature.contacts.presentation.blocklist

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactBlocklist
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.mapper.toUiState
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsEffect
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BlockedContactsViewModel(
    private val observeContactBlocklist: ObserveContactBlocklistUseCase,
    private val blockContact: BlockContactUseCase,
    private val unblockContact: UnblockContactUseCase,
    private val observeProfilePictures: ObserveContactProfilePicturesUseCase
) : BaseViewModel() {
    private val actionState = MutableStateFlow(BlockedContactsActionState())

    val uiState: StateFlow<BlockedContactsUiState> =
        combine(
            observeBlocklistWithProfilePictures(),
            actionState
        ) { snapshot, action ->
            snapshot.blocklist.toUiState(
                profilePictures = snapshot.profilePictures,
                showAddContacts = action.showAddContacts,
                phoneNumber = action.phoneNumber,
                phoneNumberError = action.phoneNumberError,
                processingContactId = action.processingContactId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BlockedContactsUiState()
        )

    private val _effects = Channel<BlockedContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onUiEvent(event: BlockedContactsUiEvent) {
        when (event) {
            BlockedContactsUiEvent.BackClicked -> navigator.popBackStack()
            BlockedContactsUiEvent.AddContactClicked -> showAddContacts()
            BlockedContactsUiEvent.AddContactsDismissed -> dismissAddContacts()
            is BlockedContactsUiEvent.PhoneNumberChanged -> updatePhoneNumber(event.value)
            BlockedContactsUiEvent.BlockPhoneNumberClicked -> blockPhoneNumber()
            is BlockedContactsUiEvent.BlockContactClicked -> block(event.contactId)
            is BlockedContactsUiEvent.UnblockContactClicked -> unblock(event.contactId)
        }
    }

    private fun observeBlocklistWithProfilePictures(): Flow<BlocklistSnapshot> =
        observeContactBlocklist()
            .flatMapLatest { blocklist ->
                val contacts = blocklist.blockedContacts + blocklist.availableContacts
                observeProfilePictures(contacts.mapTo(mutableSetOf(), Contact::id))
                    .map { profilePictures ->
                        BlocklistSnapshot(
                            blocklist = blocklist,
                            profilePictures = profilePictures
                        )
                    }
            }

    private fun showAddContacts() {
        actionState.update {
            it.copy(
                showAddContacts = true,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    private fun dismissAddContacts() {
        actionState.update {
            it.copy(
                showAddContacts = false,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    private fun updatePhoneNumber(phoneNumber: String) {
        actionState.update {
            it.copy(
                phoneNumber = phoneNumber,
                phoneNumberError = null
            )
        }
    }

    private fun block(contactId: String) {
        updateContact(contactId) {
            blockContact(contactId)
        }
    }

    private fun blockPhoneNumber() {
        val state = actionState.value
        val phoneNumber = state.phoneNumber.trim()
        if (phoneNumber.isEmpty() || state.processingContactId != null) return

        viewModelScope.launch {
            actionState.update {
                it.copy(
                    processingContactId = PHONE_NUMBER_OPERATION_ID,
                    phoneNumberError = null
                )
            }

            blockContact
                .byPhoneNumber(phoneNumber)
                .onSuccess {
                    actionState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    actionState.update {
                        it.copy(
                            phoneNumberError = error.message ?: "Phone number could not be blocked"
                        )
                    }
                }

            actionState.update { it.copy(processingContactId = null) }
        }
    }

    private fun unblock(contactId: String) {
        updateContact(contactId) {
            unblockContact(contactId)
        }
    }

    private fun updateContact(
        contactId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (actionState.value.processingContactId != null) return

        viewModelScope.launch {
            actionState.update { it.copy(processingContactId = contactId) }

            operation()
                .onSuccess {
                    actionState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    _effects.send(
                        BlockedContactsEffect.ShowError(
                            message = error.message ?: "Blocked contacts could not be updated"
                        )
                    )
                }

            actionState.update { it.copy(processingContactId = null) }
        }
    }

    private data class BlocklistSnapshot(
        val blocklist: ContactBlocklist,
        val profilePictures: Map<String, ByteArray?>
    )

    private data class BlockedContactsActionState(
        val showAddContacts: Boolean = false,
        val phoneNumber: String = "",
        val phoneNumberError: String? = null,
        val processingContactId: String? = null
    )

    private companion object {
        const val PHONE_NUMBER_OPERATION_ID = "phone-number"
    }
}
