package com.cbgm.sparrow.feature.polls.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.polls.presentation.create.component.PollMediaEditor
import com.cbgm.sparrow.feature.polls.presentation.create.component.PollOptionsEditor
import com.cbgm.sparrow.feature.polls.presentation.create.component.PollSettingsSection
import com.cbgm.sparrow.feature.polls.presentation.create.component.previewPollMediaSelections
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiEvent
import com.cbgm.sparrow.feature.polls.presentation.create.model.CreatePollUiState
import com.cbgm.sparrow.feature.polls.presentation.create.model.PollOptionEditorUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_polls_create
import com.cbgm.sparrow.resources.feature_polls_create_title
import com.cbgm.sparrow.resources.feature_polls_description
import com.cbgm.sparrow.resources.feature_polls_description_placeholder
import com.cbgm.sparrow.resources.feature_polls_infos
import com.cbgm.sparrow.resources.feature_polls_media
import com.cbgm.sparrow.resources.feature_polls_options
import com.cbgm.sparrow.resources.feature_polls_question
import com.cbgm.sparrow.resources.feature_polls_question_placeholder
import com.cbgm.sparrow.resources.feature_polls_settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreatePollScreen(
    uiState: CreatePollUiState,
    onUiEvent: (CreatePollUiEvent) -> Unit,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CreatePollTopBar(
                containerColor = containerColor,
                canCreate = uiState.canCreate,
                onBack = { onUiEvent(CreatePollUiEvent.BackClicked) },
                onCreate = { onUiEvent(CreatePollUiEvent.CreateClicked) }
            )
        }
    ) { innerPadding, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.screenPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "info") {
                InfoSection(
                    title = stringResource(Res.string.feature_polls_infos),
                    question = uiState.question,
                    description = uiState.description,
                    onUiEvent = onUiEvent
                )
            }
            item(key = "options") {
                OptionsSection(
                    title = stringResource(Res.string.feature_polls_options),
                    options = uiState.options,
                    onUiEvent = onUiEvent
                )
            }
            item(key = "media") {
                MediaSection(
                    title = stringResource(Res.string.feature_polls_media),
                    media = uiState.media,
                    onUiEvent = onUiEvent,
                    onAddMedia = onAddMedia
                )
            }
            item(key = "settings") {
                SettingsSection(
                    title = stringResource(Res.string.feature_polls_settings),
                    expiryEnabled = uiState.expiryEnabled,
                    expiryMinutes = uiState.expiryMinutes,
                    expiryInvalid = uiState.expiryInvalid,
                    allowMultipleSelection = uiState.allowMultipleSelection,
                    allowVoteChange = uiState.allowVoteChange,
                    isAnonymous = uiState.isAnonymous,
                    onUiEvent = onUiEvent
                )
            }
        }
    }
}

@Composable
private fun CreatePollTopBar(
    containerColor: Color,
    canCreate: Boolean,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text(
                text = stringResource(Res.string.feature_polls_create_title),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        actions = {
            IconButton(onClick = onCreate, enabled = canCreate) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(Res.string.feature_polls_create)
                )
            }
        }
    )
}

@Composable
private fun InfoSection(
    title: String,
    question: String,
    description: String,
    onUiEvent: (CreatePollUiEvent) -> Unit
) {
    PollSection(title = title) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small)
        ) {
            SparrowInputField(
                value = question,
                onValueChange = { onUiEvent(CreatePollUiEvent.QuestionChanged(it)) },
                label = stringResource(Res.string.feature_polls_question),
                placeholderText = stringResource(Res.string.feature_polls_question_placeholder),
                minLines = 1,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            SparrowInputField(
                value = description,
                onValueChange = { onUiEvent(CreatePollUiEvent.DescriptionChanged(it)) },
                label = stringResource(Res.string.feature_polls_description),
                placeholderText = stringResource(Res.string.feature_polls_description_placeholder),
                minLines = 1,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OptionsSection(
    title: String,
    options: List<PollOptionEditorUi>,
    onUiEvent: (CreatePollUiEvent) -> Unit
) {
    PollSection(title = title) {
        PollOptionsEditor(
            options = options,
            onOptionChanged = { id, value ->
                onUiEvent(CreatePollUiEvent.OptionChanged(id, value))
            },
            onRemoveOption = { onUiEvent(CreatePollUiEvent.RemoveOptionClicked(it)) },
            onAddOption = { onUiEvent(CreatePollUiEvent.AddOptionClicked) }
        )
    }
}

@Composable
private fun MediaSection(
    title: String,
    media: List<VisualMediaSelectionUi>,
    onUiEvent: (CreatePollUiEvent) -> Unit,
    onAddMedia: () -> Unit
) {
    PollSection(title = title) {
        PollMediaEditor(
            media = media,
            onAddMedia = onAddMedia,
            onRemoveMedia = { onUiEvent(CreatePollUiEvent.RemoveMediaClicked(it)) }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    expiryEnabled: Boolean,
    expiryMinutes: String,
    expiryInvalid: Boolean,
    allowMultipleSelection: Boolean,
    allowVoteChange: Boolean,
    isAnonymous: Boolean,
    onUiEvent: (CreatePollUiEvent) -> Unit
) {
    PollSection(title = title) {
        PollSettingsSection(
            expiryEnabled = expiryEnabled,
            expiryMinutes = expiryMinutes,
            expiryInvalid = expiryInvalid,
            allowMultipleSelection = allowMultipleSelection,
            allowVoteChange = allowVoteChange,
            isAnonymous = isAnonymous,
            onExpiryEnabledChanged = {
                onUiEvent(CreatePollUiEvent.ExpiryEnabledChanged(it))
            },
            onExpiryMinutesChanged = { onUiEvent(CreatePollUiEvent.ExpiryMinutesChanged(it)) },
            onMultipleSelectionChanged = {
                onUiEvent(CreatePollUiEvent.MultipleSelectionChanged(it))
            },
            onVoteChangeChanged = { onUiEvent(CreatePollUiEvent.VoteChangeChanged(it)) },
            onAnonymousChanged = { onUiEvent(CreatePollUiEvent.AnonymousChanged(it)) }
        )
    }
}

@Preview
@Composable
private fun CreatePollTopBarPreview() {
    SparrowTheme {
        CreatePollTopBar(
            containerColor = MaterialTheme.colorScheme.background,
            canCreate = true,
            onBack = {},
            onCreate = {}
        )
    }
}

@Preview
@Composable
private fun CreatePollScreenDefaultPreview() {
    SparrowTheme {
        CreatePollScreen(
            uiState = CreatePollUiState(
                options = listOf(
                    PollOptionEditorUi("1"),
                    PollOptionEditorUi("2")
                )
            ),
            onUiEvent = {},
            onAddMedia = {}
        )
    }
}

@Composable
private fun PollSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.base.div(2),
                    bottom = MaterialTheme.spacing.base
                )
        )

        SparrowCardNoAnimation {
            content()
        }
    }
}

@Preview
@Composable
private fun CreatePollScreenPreview() {
    SparrowTheme {
        CreatePollScreen(
            uiState =
                CreatePollUiState(
                    question = "What should we do this weekend?",
                    description = "Let's decide together!",
                    options =
                        listOf(
                            PollOptionEditorUi("1", "Go hiking"),
                            PollOptionEditorUi("2", "Visit a city"),
                            PollOptionEditorUi("3", "Stay at home")
                        ),
                    media = previewPollMediaSelections(),
                    expiryEnabled = true,
                    expiryMinutes = "3600",
                    allowMultipleSelection = true,
                    allowVoteChange = true,
                    isAnonymous = true,
                    canCreate = true
                ),
            onUiEvent = {},
            onAddMedia = {}
        )
    }
}
