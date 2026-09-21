package com.cbgm.sparrow.feature.identity.presentation.setup.profile

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.usecase.ConsumeAvatarEditResultUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The current user's profile picture belongs to Identity, not Settings. */
class IdentityProfilePictureViewModel(
    observeLocalProfilePicture: ObserveLocalProfilePictureUseCase,
    private val consumeAvatarEditResult: ConsumeAvatarEditResultUseCase,
    private val setLocalProfilePicture: SetLocalProfilePictureUseCase,
    private val removeLocalProfilePicture: RemoveLocalProfilePictureUseCase
) : BaseViewModel() {
    private val action = MutableStateFlow(Action())

    val uiState: StateFlow<IdentityProfilePictureUiState> =
        combine(observeLocalProfilePicture(), action) { picture, action ->
            IdentityProfilePictureUiState(
                hasPicture = picture.hasPicture,
                isSaving = action.isSaving,
                errorMessage = action.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = IdentityProfilePictureUiState()
        )

    fun savePicture(result: AvatarEditResult) {
        if (action.value.isSaving) return
        viewModelScope.launch {
            action.value = Action(isSaving = true)
            consumeAvatarEditResult(result)
                .fold(
                    onSuccess = { bytes -> setLocalProfilePicture(bytes) },
                    onFailure = { error -> Result.failure(error) }
                ).onSuccess {
                    action.value = Action()
                }.onFailure { error ->
                    SparrowLog.error("IdentityProfilePictureViewModel", "Profile picture change failed", error)
                    action.value = Action(errorMessage = error.message ?: "Profile picture could not be saved")
                }
        }
    }

    fun removePicture() {
        if (action.value.isSaving) return
        viewModelScope.launch {
            action.value = Action(isSaving = true)
            removeLocalProfilePicture()
                .onSuccess { action.value = Action() }
                .onFailure { error ->
                    SparrowLog.error("IdentityProfilePictureViewModel", "Profile picture change failed", error)
                    action.value = Action(errorMessage = error.message ?: "Profile picture could not be removed")
                }
        }
    }

    private data class Action(
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )
}
