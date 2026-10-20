package com.cbgm.sparrow.feature.identity.presentation.details

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.presentation.details.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.details.model.IdentityDetailsUiEvent
import com.cbgm.sparrow.feature.identity.presentation.details.model.IdentityDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityDetailsViewModel(
    private val getPublicIdentity: GetPublicIdentityUseCase
) : BaseViewModel() {
    private val _uiState =
        MutableStateFlow(IdentityDetailsUiState(isLoading = true))
    val uiState: StateFlow<IdentityDetailsUiState> = _uiState.asStateFlow()

    init {
        loadIdentity()
    }

    fun onUiEvent(event: IdentityDetailsUiEvent) {
        when (event) {
            IdentityDetailsUiEvent.BackClicked -> navigator.popBackStack()
            IdentityDetailsUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            IdentityDetailsUiEvent.RetryClicked -> loadIdentity()
        }
    }

    private fun loadIdentity() {
        viewModelScope.launch {
            _uiState.value = IdentityDetailsUiState(isLoading = true)
            getPublicIdentity()
                .onSuccess { publicIdentity ->
                    if (publicIdentity != null) {
                        _uiState.value = IdentityDetailsUiState(
                            isLoading = false,
                            publicIdentity = publicIdentity.toUi()
                        )
                    } else {
                        _uiState.value =
                            IdentityDetailsUiState(
                                isLoading = false,
                                error = "Identity not found"
                            )
                    }
                }
                .onFailure { error ->
                    _uiState.value = IdentityDetailsUiState(
                        error = error.message ?: "Failed to load identity keys",
                        isLoading = false
                    )
                }
        }
    }
}
