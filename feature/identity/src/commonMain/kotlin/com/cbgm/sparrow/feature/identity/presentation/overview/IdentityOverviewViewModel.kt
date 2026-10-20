package com.cbgm.sparrow.feature.identity.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.flatMap
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.usecase.ConsumeAvatarEditResultUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityBackupStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.presentation.backup.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi
import com.cbgm.sparrow.feature.identity.presentation.overview.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.overview.model.IdentityOverviewUiEvent
import com.cbgm.sparrow.feature.identity.presentation.overview.model.IdentityOverviewUiState
import com.cbgm.sparrow.feature.identity.presentation.overview.profile.IdentityProfilePictureUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_overview_load_failed
import com.cbgm.sparrow.resources.feature_identity_picture_remove_failed
import com.cbgm.sparrow.resources.feature_identity_picture_save_failed
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class IdentityOverviewViewModel(
    private val getPublicIdentity: GetPublicIdentityUseCase,
    private val getLocalPhoneNumber: GetLocalPhoneNumberUseCase,
    private val getBackupStatus: GetIdentityBackupStatusUseCase,
    private val observeLocalProfilePicture: ObserveLocalProfilePictureUseCase,
    private val consumeAvatarEditResult: ConsumeAvatarEditResultUseCase,
    private val setLocalProfilePicture: SetLocalProfilePictureUseCase,
    private val removeLocalProfilePicture: RemoveLocalProfilePictureUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<IdentityOverviewUiState>(IdentityOverviewUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var backupStatus = BackupStateUi.NOT_BACKED_UP
    private var loadJob: Job? = null
    private var backupJob: Job? = null

    private var pictureState = IdentityProfilePictureUiState()
    private var pictureJob: Job? = null

    init {
        observePicture()
        refreshBackupStatus()
        load()
    }

    fun onUiEvent(event: IdentityOverviewUiEvent) {
        when (event) {
            IdentityOverviewUiEvent.RetryClicked -> {
                observePicture()
                refreshBackupStatus()
                load()
            }
            IdentityOverviewUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            IdentityOverviewUiEvent.OpenKeysClicked -> navigator.navigateTo(AppRoute.IdentityKeys)
            IdentityOverviewUiEvent.OpenBackupClicked -> navigator.navigateTo(AppRoute.IdentityBackup)
        }
    }

    private fun observePicture() {
        if (pictureJob?.isActive == true) return
        pictureJob = viewModelScope.launch {
            observeLocalProfilePicture()
                .catch { showFailure() }
                .collect { picture -> updatePicture(picture.toUi(pictureState.isSaving)) }
        }
    }

    fun savePicture(result: AvatarEditResult) {
        if (pictureState.isSaving) return
        updatePicture(pictureState.copy(isSaving = true))
        viewModelScope.launch {
            try {
                safeSuspendCall(logFailure = false) {
                    consumeAvatarEditResult(result).flatMap { bytes -> setLocalProfilePicture(bytes) }
                }.flatMap { it }.onFailure {
                    SparrowLog.showError(getString(Res.string.feature_identity_picture_save_failed))
                }
            } finally {
                updatePicture(pictureState.copy(isSaving = false))
            }
        }
    }

    fun removePicture() {
        if (pictureState.isSaving) return
        updatePicture(pictureState.copy(isSaving = true))
        viewModelScope.launch {
            try {
                safeSuspendCall(logFailure = false) { removeLocalProfilePicture() }
                    .flatMap { it }.onFailure {
                        SparrowLog.showError(getString(Res.string.feature_identity_picture_remove_failed))
                    }
            } finally {
                updatePicture(pictureState.copy(isSaving = false))
            }
        }
    }

    private fun updatePicture(picture: IdentityProfilePictureUiState) {
        pictureState = picture
        _uiState.update { state ->
            if (state is IdentityOverviewUiState.Ready) state.copy(profilePicture = picture) else state
        }
    }

    fun refreshBackupStatus() {
        if (backupJob?.isActive == true) return
        backupJob = viewModelScope.launch {
            getBackupStatus().onSuccess { status ->
                backupStatus = status.toUi()
                _uiState.update { state ->
                    if (state is IdentityOverviewUiState.Ready) {
                        state.copy(backupStatus = backupStatus)
                    } else {
                        state
                    }
                }
            }.onFailure { showFailure() }
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = IdentityOverviewUiState.Loading
        loadJob = viewModelScope.launch {
            val phone = getLocalPhoneNumber().getOrElse {
                loadFailed()
                return@launch
            }
            val identity = getPublicIdentity().getOrElse {
                loadFailed()
                return@launch
            }
            if (phone.isNullOrBlank() || identity == null) {
                loadFailed()
                return@launch
            }
            _uiState.value = IdentityOverviewUiState.Ready(phone, backupStatus, pictureState)
        }
    }

    private suspend fun loadFailed() {
        _uiState.value = IdentityOverviewUiState.Error
        showFailure()
    }

    private suspend fun showFailure() {
        SparrowLog.showError(getString(Res.string.feature_identity_overview_load_failed))
    }
}
