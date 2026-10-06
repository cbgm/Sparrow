package com.cbgm.sparrow.feature.attachments.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartSourceUi
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentContentUseCase
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttachmentViewModel(
    private val part: MessagePartUi,
    private val loadAttachmentContent: LoadAttachmentContentUseCase
) : BaseViewModel() {
    private val _localFileState = MutableStateFlow<AttachmentUiState<String>>(AttachmentUiState.Idle)
    val localFileState: StateFlow<AttachmentUiState<String>> = _localFileState.asStateFlow()

    private val _locationState = MutableStateFlow<AttachmentUiState<CurrentLocation>>(AttachmentUiState.Idle)
    val locationState: StateFlow<AttachmentUiState<CurrentLocation>> = _locationState.asStateFlow()

    private val _contactState = MutableStateFlow<AttachmentUiState<SharedContact>>(AttachmentUiState.Idle)
    val contactState: StateFlow<AttachmentUiState<SharedContact>> = _contactState.asStateFlow()

    private var localFileLoadJob: Job? = null
    private var locationLoadJob: Job? = null
    private var contactLoadJob: Job? = null

    fun loadLocalFile() {
        if (_localFileState.value is AttachmentUiState.Ready<*> || localFileLoadJob?.isActive == true) return
        localFileLoadJob =
            viewModelScope.launch {
                _localFileState.value = AttachmentUiState.Loading
                _localFileState.value =
                    loadAttachmentContent.localFile(part.id, part.sourceGroupId()).fold(
                        onSuccess = { value -> AttachmentUiState.Ready(value) },
                        onFailure = { error -> AttachmentUiState.Error(error) }
                    )
            }
    }

    fun loadLocation() {
        if (_locationState.value is AttachmentUiState.Ready<*> || locationLoadJob?.isActive == true) return
        locationLoadJob =
            viewModelScope.launch {
                _locationState.value = AttachmentUiState.Loading
                _locationState.value =
                    loadAttachmentContent.location(part.id, part.sourceGroupId()).fold(
                        onSuccess = { value -> AttachmentUiState.Ready(value) },
                        onFailure = { error -> AttachmentUiState.Error(error) }
                    )
            }
    }

    fun loadContact() {
        if (_contactState.value is AttachmentUiState.Ready<*> || contactLoadJob?.isActive == true) return
        contactLoadJob =
            viewModelScope.launch {
                _contactState.value = AttachmentUiState.Loading
                _contactState.value =
                    loadAttachmentContent.contact(part.id, part.sourceGroupId()).fold(
                        onSuccess = { value -> AttachmentUiState.Ready(value) },
                        onFailure = { error -> AttachmentUiState.Error(error) }
                    )
            }
    }
}

private fun MessagePartUi.sourceGroupId(): String? =
    when (val partSource = source) {
        MessagePartSourceUi.Message -> null
        is MessagePartSourceUi.GroupPin -> partSource.groupId
    }
