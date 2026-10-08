package com.cbgm.sparrow.feature.polls.presentation.create

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.localFilePaths
import com.cbgm.sparrow.feature.polls.domain.usecase.FinishPollUseCase
import com.cbgm.sparrow.feature.polls.presentation.create.mapper.toPoll
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiEvent
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiState
import com.cbgm.sparrow.feature.polls.presentation.create.model.PollOptionEditorUi
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_DESCRIPTION_LENGTH
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_EXPIRY_MINUTES
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_MEDIA_ITEMS
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_OPTIONS
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_QUESTION_LENGTH
import com.cbgm.sparrow.feature.polls.util.PollConstants.MIN_OPTIONS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreatePollViewModel(
    private val mediaFiles: MediaSelectionFileRepository,
    private val finishPoll: FinishPollUseCase
) : BaseViewModel() {
    private val pollId = IdGenerator.generate("poll")
    private val logger = SparrowLog.withTag("CreatePollViewModel")
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<CreatePollUiState> = _uiState.asStateFlow()
    private val mediaCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onUiEvent(event: CreatePollUiEvent) {
        if (_uiState.value.isSending) return
        when (event) {
            CreatePollUiEvent.BackClicked -> onBackClicked()
            CreatePollUiEvent.AddOptionClicked -> addOption()
            CreatePollUiEvent.CreateClicked -> createPoll()
            is CreatePollUiEvent.QuestionChanged -> updateQuestion(event.value)
            is CreatePollUiEvent.DescriptionChanged -> updateDescription(event.value)
            is CreatePollUiEvent.OptionChanged -> updateOption(event.id, event.value)
            is CreatePollUiEvent.RemoveOptionClicked -> removeOption(event.id)
            is CreatePollUiEvent.MediaSelectionChanged -> updateMedia(event.result)
            is CreatePollUiEvent.RemoveMediaClicked -> removeMedia(event.id)
            is CreatePollUiEvent.ExpiryEnabledChanged -> updateExpiryEnabled(event.enabled)
            is CreatePollUiEvent.ExpiryMinutesChanged -> updateExpiryMinutes(event.value)
            is CreatePollUiEvent.MultipleSelectionChanged -> updateMultipleSelection(event.enabled)
            is CreatePollUiEvent.VoteChangeChanged -> updateVoteChange(event.enabled)
            is CreatePollUiEvent.AnonymousChanged -> updateAnonymous(event.enabled)
        }
    }

    private fun onBackClicked() {
        val pendingMedia = _uiState.value.media
        _uiState.update { it.copy(media = emptyList()).validated() }
        deletePendingSelections(pendingMedia)
        navigator.popBackStack()
    }

    private fun updateQuestion(value: String) {
        _uiState.update { it.copy(question = value.take(MAX_QUESTION_LENGTH)).validated() }
    }

    private fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value.take(MAX_DESCRIPTION_LENGTH)).validated() }
    }

    private fun addOption() {
        _uiState.update { state ->
            if (state.options.size >= MAX_OPTIONS) return@update state
            state.copy(
                options = state.options + PollOptionEditorUi(id = IdGenerator.generate("poll-option"))
            ).validated()
        }
    }

    private fun updateOption(id: String, value: String) {
        _uiState.update { state ->
            state.copy(
                options = state.options.map { option ->
                    if (option.id == id) option.copy(text = value) else option
                }
            ).validated()
        }
    }

    private fun removeOption(id: String) {
        _uiState.update { state ->
            if (state.options.size <= MIN_OPTIONS) {
                state
            } else {
                state.copy(options = state.options.filterNot { it.id == id }).validated()
            }
        }
    }

    private fun updateMedia(result: MediaSelectionResultUi) {
        when (result) {
            MediaSelectionResultUi.Dismissed -> Unit
            is MediaSelectionResultUi.Error -> Unit
            is MediaSelectionResultUi.Selected -> applySelectedMedia(result.media.filterIsInstance<VisualMediaSelectionUi>())
        }
    }

    private fun applySelectedMedia(candidate: List<VisualMediaSelectionUi>) {
        val previous = _uiState.value.media
        val limited = candidate.take(MAX_MEDIA_ITEMS)
        val newItems = limited.filterNot { item -> previous.any { it.id == item.id } }

        if (!isValidPollMedia(limited)) {
            deletePendingSelections(newItems)
            return
        }

        val removed = previous.filterNot { current -> limited.any { it.id == current.id } }
        _uiState.update { it.copy(media = limited).validated() }
        deletePendingSelections(removed)
    }

    private fun removeMedia(id: String) {
        val removed = _uiState.value.media.firstOrNull { it.id == id } ?: return
        _uiState.update { state -> state.copy(media = state.media.filterNot { it.id == id }).validated() }
        deletePendingSelections(listOf(removed))
    }

    private fun isValidPollMedia(media: List<VisualMediaSelectionUi>): Boolean {
        if (media.size > MAX_MEDIA_ITEMS) return false
        if (media.any { it.source != MediaSourceUi.GALLERY }) return false
        if (media.map(VisualMediaSelectionUi::id).distinct().size != media.size) return false
        if (media.sumOf(VisualMediaSelectionUi::byteSize) > MessageAttachmentPolicy.MAX_TOTAL_ATTACHMENT_BYTES) return false

        return media.all { item ->
            item.type == MediaTypeUi.IMAGE &&
                item.byteSize in 1..MessageAttachmentPolicy.MAX_IMAGE_BYTES.toLong() &&
                item.mimeType.startsWith("image/") &&
                item.width != null && item.height != null
        }
    }

    private fun deletePendingSelections(media: List<VisualMediaSelectionUi>) {
        if (media.isEmpty()) return
        mediaCleanupScope.launch {
            media.flatMap { it.localFilePaths }
                .distinct()
                .forEach { path -> runCatching { mediaFiles.delete(path) } }
        }
    }

    private fun updateExpiryEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                expiryEnabled = enabled,
                expiryMinutes = if (enabled) state.expiryMinutes else "",
                expiryInvalid = false
            ).validated()
        }
    }

    private fun updateExpiryMinutes(value: String) {
        val minutes = value.filter(Char::isDigit)
        _uiState.update { it.copy(expiryMinutes = minutes).validated() }
    }

    private fun updateMultipleSelection(enabled: Boolean) {
        _uiState.update { it.copy(allowMultipleSelection = enabled).validated() }
    }

    private fun updateVoteChange(enabled: Boolean) {
        _uiState.update { it.copy(allowVoteChange = enabled).validated() }
    }

    private fun updateAnonymous(enabled: Boolean) {
        _uiState.update { it.copy(isAnonymous = enabled).validated() }
    }

    private fun createPoll() {
        val state = _uiState.value.validated()
        _uiState.value = state
        if (!state.canCreate) return
        _uiState.value = state.copy(isSending = true, canCreate = false)
        viewModelScope.launch {
            try {
                finishPoll(
                    state.toPoll(
                        id = pollId,
                        nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
                    .onSuccess {
                        _uiState.update { it.copy(media = emptyList()) }
                        navigator.popBackStack()
                    }
                    .onFailure { error ->
                        logger.warn(error) { "Poll could not be finished" }
                    }
            } finally {
                _uiState.update { it.copy(isSending = false).validated() }
            }
        }
    }

    private fun CreatePollUiState.validated(): CreatePollUiState {
        val validOptions = options.size in MIN_OPTIONS..MAX_OPTIONS && options.all { it.text.isNotBlank() }
        val parsedExpiryMinutes = expiryMinutes.toLongOrNull()
        val validExpiry =
            !expiryEnabled ||
                parsedExpiryMinutes != null && parsedExpiryMinutes in 1L..MAX_EXPIRY_MINUTES

        return copy(
            expiryInvalid = expiryEnabled && expiryMinutes.isNotBlank() && !validExpiry,
            canCreate = !isSending && question.isNotBlank() && validOptions && validExpiry && isValidPollMedia(media)
        )
    }

    override fun onCleared() {
        deletePendingSelections(_uiState.value.media)
    }

    private fun initialState(): CreatePollUiState =
        CreatePollUiState(
            options = List(MIN_OPTIONS) { PollOptionEditorUi(id = IdGenerator.generate("poll-option")) }
        ).validated()
}
