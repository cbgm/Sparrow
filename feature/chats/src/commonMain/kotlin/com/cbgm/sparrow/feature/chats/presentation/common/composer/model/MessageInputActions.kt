package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi

data class MessageInputActions(
    val onValueChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onCancelPreview: () -> Unit = {},
    val onSelectionClick: (MediaSourceUi) -> Unit = {},
    val onMediaRemove: (String) -> Unit = {},
    val onClickCamera: () -> Unit = {},
    val onClickFile: () -> Unit = {},
    val onClickGallery: () -> Unit = {},
    val onClickContact: () -> Unit = {},
    val onClickLocation: () -> Unit = {},
    val onClickPoll: (() -> Unit)? = null,
    val onClickActivateExpenses: (() -> Unit)? = null,
    val onClickCloseExpenses: (() -> Unit)? = null,
    val onClickAddExpense: (() -> Unit)? = null,
    val isExpensesActive: Boolean = false,
    val onVoiceSendClick: () -> Unit = {}
)
