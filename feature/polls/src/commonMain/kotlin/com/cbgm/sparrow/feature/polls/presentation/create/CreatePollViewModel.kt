package com.cbgm.sparrow.feature.polls.presentation.create

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.localFilePaths
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiEvent
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiState
import com.cbgm.sparrow.feature.polls.presentation.create.model.PollOptionEditorUi
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_DESCRIPTION_LENGTH
import com.cbgm.sparrow.feature.polls.util.PollConstants.MAX_MEDIA_ITEMS
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class CreatePollViewModel(
    private val mediaFiles: MediaSelectionFileRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<CreatePollUiState> = _uiState.asStateFlow()
    private val mediaCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onUiEvent(event: CreatePollUiEvent) {
        when (event) {
            CreatePollUiEvent.BackClicked -> onBackClicked()
            CreatePollUiEvent.AddOptionClicked -> addOption()
            CreatePollUiEvent.CreateClicked -> validateForCreate()
            CreatePollUiEvent.ExpiryCleared -> updateExpiryEnabled(false)
            is CreatePollUiEvent.QuestionChanged -> updateQuestion(event.value)
            is CreatePollUiEvent.DescriptionChanged -> updateDescription(event.value)
            is CreatePollUiEvent.OptionChanged -> updateOption(event.id, event.value)
            is CreatePollUiEvent.RemoveOptionClicked -> removeOption(event.id)
            is CreatePollUiEvent.MediaSelectionChanged -> updateMedia(event.result)
            is CreatePollUiEvent.RemoveMediaClicked -> removeMedia(event.id)
            is CreatePollUiEvent.ExpiryEnabledChanged -> updateExpiryEnabled(event.enabled)
            is CreatePollUiEvent.ExpiryDateChanged -> updateExpiryDate(event.value)
            is CreatePollUiEvent.ExpiryTimeChanged -> updateExpiryTime(event.value)
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
            when (item.type) {
                MediaTypeUi.IMAGE ->
                    item.byteSize in 1..MessageAttachmentPolicy.MAX_IMAGE_BYTES.toLong() &&
                        item.mimeType.startsWith("image/") &&
                        item.width != null && item.height != null

                MediaTypeUi.VIDEO ->
                    item.byteSize in 1..MessageAttachmentPolicy.MAX_VIDEO_BYTES &&
                        item.mimeType.startsWith("video/")
            }
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
            if (enabled) {
                state.copy(expiryEnabled = true).withResolvedExpiry()
            } else {
                state.copy(
                    expiryEnabled = false,
                    expiryDate = "",
                    expiryTime = "",
                    expiresAtEpochMilliseconds = null,
                    expiryInvalid = false
                ).validated()
            }
        }
    }

    private fun updateExpiryDate(value: String) {
        _uiState.update { it.copy(expiryDate = value).withResolvedExpiry() }
    }

    private fun updateExpiryTime(value: String) {
        _uiState.update { it.copy(expiryTime = value).withResolvedExpiry() }
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

    private fun validateForCreate() {
        _uiState.update { it.withResolvedExpiry().validated() }
    }

    private fun CreatePollUiState.withResolvedExpiry(): CreatePollUiState {
        if (!expiryEnabled) return validated()

        val fieldsComplete = expiryDate.isNotBlank() && expiryTime.isNotBlank()
        val resolved = if (fieldsComplete) resolvePollExpiry(expiryDate, expiryTime) else null
        val valid = resolved != null && resolved > SystemClock.nowEpochMilliseconds()

        return copy(
            expiresAtEpochMilliseconds = resolved?.takeIf { valid },
            expiryInvalid = fieldsComplete && !valid
        ).validated()
    }

    private fun CreatePollUiState.validated(): CreatePollUiState {
        val validOptions = options.size >= MIN_OPTIONS && options.all { it.text.isNotBlank() }
        val validExpiry = !expiryEnabled || expiresAtEpochMilliseconds != null
        return copy(canCreate = question.isNotBlank() && validOptions && validExpiry)
    }

    override fun onCleared() {
        deletePendingSelections(_uiState.value.media)
    }

    private fun initialState(): CreatePollUiState =
        CreatePollUiState(
            options = List(MIN_OPTIONS) { PollOptionEditorUi(id = IdGenerator.generate("poll-option")) }
        ).validated()

    private fun resolvePollExpiry(date: String, time: String): Long? =
        runCatching {
            val localDate = LocalDate.parse(date.trim())
            val localTime = LocalTime.parse(time.trim())
            LocalDateTime(localDate, localTime)
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        }.getOrNull()
}
