package com.cbgm.sparrow.feature.autoreply.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowFloatingButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowRadio
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.component.SwipeActionTone
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyEditorUiState
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyItemUi
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyUiEvent
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_auto_reply
import com.cbgm.sparrow.resources.feature_auto_reply_add
import com.cbgm.sparrow.resources.feature_auto_reply_edit
import com.cbgm.sparrow.resources.feature_auto_reply_message
import com.cbgm.sparrow.resources.feature_auto_reply_name
import com.cbgm.sparrow.resources.feature_auto_reply_off
import com.cbgm.sparrow.resources.feature_auto_reply_save
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoReplySettingsScreen(
    uiState: AutoReplyUiState,
    onUiEvent: (AutoReplyUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            SparrowTopBar(
                title = {
                    Text(
                        text = stringResource(Res.string.feature_auto_reply)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(AutoReplyUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = SparrowIcons.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                containerColor = color
            )
        },
        floatingActionButton = {
            SparrowFloatingButton(
                icon = SparrowIcons.Add,
                onClick = { onUiEvent(AutoReplyUiEvent.AddClicked) },
                contentDescription = stringResource(Res.string.feature_auto_reply_add)
            )
        }
    ) { innerPadding, listState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding =
                PaddingValues(
                    top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.screenPadding,
                    bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding,
                    start = MaterialTheme.spacing.screenPadding,
                    end = MaterialTheme.spacing.screenPadding
                )
        ) {
            item(key = "auto-reply-settings") {
                Column {
                    AutoReplyOffItem(
                        selected = uiState.replies.none { it.isActive },
                        onClick = { onUiEvent(AutoReplyUiEvent.OffClicked) }
                    )
                    uiState.replies.forEach { reply ->
                        HorizontalDivider(
                            modifier = Modifier.padding(start = Dimens.SettingsScreen.primaryIconSize + MaterialTheme.spacing.small),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                        )
                        androidx.compose.runtime.key(reply.id) {
                            AutoReplyItem(
                                reply = reply,
                                onActivate = { onUiEvent(AutoReplyUiEvent.ActivateClicked(reply.id)) },
                                onEdit = { onUiEvent(AutoReplyUiEvent.EditClicked(reply.id)) },
                                onDelete = { onUiEvent(AutoReplyUiEvent.DeleteClicked(reply.id)) }
                            )
                        }
                    }
                }
            }
        }
    }

    AutoReplyEditorDialog(
        editor = uiState.editor,
        isSaving = uiState.isSaving,
        onNameChanged = {
            onUiEvent(AutoReplyUiEvent.EditorNameChanged(it))
        },
        onTextChanged = {
            onUiEvent(AutoReplyUiEvent.EditorTextChanged(it))
        },
        onSave = { onUiEvent(AutoReplyUiEvent.EditorSaveClicked) },
        onDismiss = { onUiEvent(AutoReplyUiEvent.EditorDismissed) }
    )
}

@Composable
private fun AutoReplyOffItem(selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize)
        )
        Spacer(Modifier.size(MaterialTheme.spacing.small))
        Text(
            text = stringResource(Res.string.feature_auto_reply_off),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        SparrowRadio(selected = selected, onClick = null)
    }
}

@Composable
private fun AutoReplyItem(
    reply: AutoReplyItemUi,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SparrowSwipeRevealItem(
        actions = listOf(
            SwipeRevealAction(
                tone = SwipeActionTone.NEUTRAL,
                onClick = onEdit
            ) {
                Icon(
                    imageVector = SparrowIcons.Edit,
                    contentDescription = stringResource(Res.string.feature_auto_reply_edit)
                )
            },
            SwipeRevealAction(
                tone = SwipeActionTone.DESTRUCTIVE,
                onClick = onDelete
            ) {
                Icon(
                    imageVector = SparrowIcons.Delete,
                    contentDescription = null
                )
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = if (reply.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize)
            )
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reply.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = reply.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SparrowRadio(selected = reply.isActive, onClick = null)
        }
    }
}

@Composable
private fun AutoReplyEditorDialog(
    editor: AutoReplyEditorUiState?,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    editor?.let {
        SparrowAlertDialog(
            isVisible = true,
            onDismissRequest = { if (!isSaving) onDismiss() },
            title =
                stringResource(
                    if (editor.isEditing) {
                        Res.string.feature_auto_reply_edit
                    } else {
                        Res.string.feature_auto_reply_add
                    }
                ),
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SparrowInputField(
                        value = editor.name,
                        onValueChange = onNameChanged,
                        label = stringResource(Res.string.feature_auto_reply_name),
                        isSingleLine = true,
                        isEnabled = !isSaving,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            )
                    )

                    SparrowInputField(
                        value = editor.text,
                        onValueChange = onTextChanged,
                        label = stringResource(Res.string.feature_auto_reply_message),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                        minLines = 1,
                        maxLines = 7,
                        isEnabled = !isSaving,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            )
                    )
                }
            },
            confirmButton = {
                SparrowDialogAction(
                    onClick = onSave,
                    text = stringResource(Res.string.feature_auto_reply_save),
                    enabled = editor.canSave && !isSaving
                )
            },
            dismissButton = {
                SparrowDialogAction(
                    onClick = onDismiss,
                    text = stringResource(Res.string.base_cancel),
                    enabled = !isSaving,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Preview
@Composable
private fun AutoReplySettingsScreenPreview() {
    SparrowTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AutoReplySettingsScreen(
                uiState =
                    AutoReplyUiState(
                        replies =
                            listOf(
                                AutoReplyItemUi(
                                    id = "1",
                                    name = "Vacation",
                                    text = "I'm currently on vacation and will reply when I'm back.",
                                    isActive = true
                                ),
                                AutoReplyItemUi(
                                    id = "2",
                                    name = "Meeting",
                                    text = "I'm currently in a meeting.",
                                    isActive = false
                                )
                            )
                    ),
                onUiEvent = {}
            )
        }
    }
}
