package com.cbgm.sparrow.feature.autoreply.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ActivateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.CreateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.DeactivateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.DeleteAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ObserveAutoRepliesUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.UpdateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.presentation.mapper.toUiItem
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyEditorUiState
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyUiEvent
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutoReplyViewModel(
    observeAutoReplies: ObserveAutoRepliesUseCase,
    private val createAutoReply: CreateAutoReplyUseCase,
    private val updateAutoReply: UpdateAutoReplyUseCase,
    private val deleteAutoReply: DeleteAutoReplyUseCase,
    private val activateAutoReply: ActivateAutoReplyUseCase,
    private val deactivateAutoReply: DeactivateAutoReplyUseCase
) : BaseViewModel() {
    private val editorState = MutableStateFlow<AutoReplyEditorUiState?>(null)
    private val savingState = MutableStateFlow(false)
    private val replies = observeAutoReplies()

    val uiState: StateFlow<AutoReplyUiState> =
        combine(replies, editorState, savingState) { replies, editor, isSaving ->
            AutoReplyUiState(
                replies = replies.map { it.toUiItem() },
                editor = editor,
                isSaving = isSaving
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AutoReplyUiState()
        )

    fun onUiEvent(event: AutoReplyUiEvent) {
        when (event) {
            AutoReplyUiEvent.BackClicked -> navigator.popBackStack()
            AutoReplyUiEvent.AddClicked -> editorState.value = AutoReplyEditorUiState()
            AutoReplyUiEvent.OffClicked -> deactivate()
            AutoReplyUiEvent.EditorDismissed -> closeEditor()
            AutoReplyUiEvent.EditorSaveClicked -> saveEditor()
            is AutoReplyUiEvent.ActivateClicked -> activate(event.id)
            is AutoReplyUiEvent.EditClicked -> edit(event.id)
            is AutoReplyUiEvent.DeleteClicked -> delete(event.id)
            is AutoReplyUiEvent.EditorNameChanged ->
                editorState.update { editor -> editor?.copy(name = event.value) }
            is AutoReplyUiEvent.EditorTextChanged ->
                editorState.update { editor -> editor?.copy(text = event.value) }
        }
    }

    private fun activate(id: String) {
        if (uiState.value.replies.any { it.id == id && it.isActive }) return
        viewModelScope.launch {
            activateAutoReply(id)
                .onFailure { error -> showError(error) }
        }
    }

    private fun deactivate() {
        if (uiState.value.replies.none { it.isActive }) return
        viewModelScope.launch {
            deactivateAutoReply()
                .onFailure { error -> showError(error) }
        }
    }

    private fun edit(id: String) {
        val reply = uiState.value.replies.firstOrNull { it.id == id } ?: return
        editorState.value =
            AutoReplyEditorUiState(
                id = reply.id,
                name = reply.name,
                text = reply.text
            )
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            deleteAutoReply(id)
                .onFailure { error -> showError(error) }
        }
    }

    private fun saveEditor() {
        val editor = editorState.value ?: return
        if (!editor.canSave || savingState.value) return

        val name = editor.name.trim()
        val text = editor.text.trim()

        viewModelScope.launch {
            savingState.value = true
            val result =
                editor.id?.let { id ->
                    updateAutoReply(
                        id = id,
                        name = name,
                        text = text
                    )
                } ?: createAutoReply(
                    name = name,
                    text = text
                )

            savingState.value = false
            result
                .onSuccess { closeEditor() }
                .onFailure { error -> showError(error) }
        }
    }

    private fun closeEditor() {
        if (!savingState.value) {
            editorState.value = null
        }
    }

    private fun showError(error: Throwable) {
        SparrowLog.error("AutoReplyViewModel", "Auto reply action failed", error)
    }
}
