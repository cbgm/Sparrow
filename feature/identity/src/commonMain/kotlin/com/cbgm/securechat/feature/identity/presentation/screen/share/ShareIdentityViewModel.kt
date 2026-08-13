package com.cbgm.securechat.feature.identity.presentation.screen.share

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentity
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiEvent
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareIdentityViewModel(
    private val createSharedIdentity: CreateSharedIdentity
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ShareIdentityUiState())

    val uiState: StateFlow<ShareIdentityUiState> = _uiState.asStateFlow()

    init {
        generateSharedIdentity()
    }

    fun onUiEvent(event: ShareIdentityUiEvent) {
        when (event) {
            ShareIdentityUiEvent.GenerateClicked -> generateSharedIdentity()
            ShareIdentityUiEvent.BackClicked -> navigateBack()
            ShareIdentityUiEvent.ShareClicked -> Unit
        }
    }

    private fun navigateBack() {
        navigator.popBackStack()
    }

    private fun generateSharedIdentity() {
        if (_uiState.value.isGenerating) {
            return
        }

        _uiState.update { current ->
            current.copy(
                isGenerating = true,
                encodedIdentity = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            createSharedIdentity()
                .onSuccess { encodedIdentity ->
                    _uiState.update { current ->
                        current.copy(
                            isGenerating = false,
                            encodedIdentity = encodedIdentity,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isGenerating = false,
                            encodedIdentity = null,
                            errorMessage = error.message ?: "Failed to create shared identity"
                        )
                    }
                }
        }
    }
}
