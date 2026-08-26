package com.cbgm.sparrow.feature.settings.presentation.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.errors.components.ClearDeveloperErrorsDialog
import com.cbgm.sparrow.feature.settings.presentation.errors.components.DeveloperErrorItem
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorLogUiEvent
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorLogUiState
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_clear_saved_errors
import com.cbgm.sparrow.resources.feature_settings_error_log
import com.cbgm.sparrow.resources.feature_settings_error_log_empty
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperErrorLogScreen(
    uiState: DeveloperErrorLogUiState,
    onUiEvent: (DeveloperErrorLogUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                containerColor = containerColor,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding, listState ->
        Content(
            listState = listState,
            innerPadding = innerPadding,
            onUiEvent = onUiEvent,
            uiState = uiState
        )
    }

    if (uiState.showClearConfirmation) {
        ClearDeveloperErrorsDialog(
            onConfirm = { onUiEvent(DeveloperErrorLogUiEvent.ClearErrorsConfirmed) },
            onDismiss = { onUiEvent(DeveloperErrorLogUiEvent.ClearErrorsDismissed) }
        )
    }
}

@Composable
private fun Content(
    listState: LazyListState,
    innerPadding: PaddingValues,
    onUiEvent: (DeveloperErrorLogUiEvent) -> Unit,
    uiState: DeveloperErrorLogUiState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.medium
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        item(key = "clear-errors") {
            SparrowDestructiveButton(
                onClick = { onUiEvent(DeveloperErrorLogUiEvent.ClearErrorsClicked) },
                enabled = uiState.errors.isNotEmpty() && !uiState.isClearing,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(
                        text = stringResource(Res.string.feature_settings_clear_saved_errors),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        if (uiState.errors.isEmpty()) {
            item(key = "empty-errors") {
                Text(
                    text = stringResource(Res.string.feature_settings_error_log_empty),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = uiState.errors,
                key = { error -> error.id }
            ) { error ->
                DeveloperErrorItem(error = error)
            }
        }
    }
}

@Composable
private fun TopBar(
    containerColor: Color,
    onUiEvent: (DeveloperErrorLogUiEvent) -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_error_log),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = { onUiEvent(DeveloperErrorLogUiEvent.BackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Preview
@Composable
private fun DeveloperErrorLogScreenPreview() {
    SparrowTheme {
        DeveloperErrorLogScreen(
            uiState =
                DeveloperErrorLogUiState(
                    errors =
                        listOf(
                            DeveloperErrorUi(
                                id = "1",
                                timestamp = "25.08.2026 03:35:42.123",
                                tag = "DefaultOutboxRunner",
                                message = "Outgoing message failed",
                                exceptionType = "IllegalStateException",
                                stackTrace = "java.lang.IllegalStateException: Invalid outbox transition"
                            ),
                            DeveloperErrorUi(
                                id = "2",
                                timestamp = "25.08.2026 03:31:07.451",
                                tag = "Transport",
                                message = "Connection timed out",
                                exceptionType = "ConnectTimeoutException"
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
