package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_expenses_add_another
import com.cbgm.sparrow.resources.feature_expenses_add_multiple
import com.cbgm.sparrow.resources.feature_expenses_queued
import com.cbgm.sparrow.resources.feature_expenses_send_all
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateExpenseScreen(
    uiState: CreateExpenseUiState,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                uiState = uiState,
                onUiEvent = onUiEvent,
                containerColor = containerColor,
                focusManager = focusManager
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && !uiState.isSending) {
                FloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        onUiEvent(CreateExpenseUiEvent.AddAnotherClicked)
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(Res.string.feature_expenses_add_another)
                    )
                }
            }
        }
    ) { innerPadding, listState ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.screenPadding),
            state = listState,
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "editor") {
                ExpenseDraftCard(
                    draft = uiState.draft,
                    participants = uiState.participants,
                    currencyCode = uiState.currencyCode,
                    enabled = !uiState.isLoading && !uiState.isSending,
                    onUiEvent = onUiEvent
                )
            }
            if (uiState.queued.isNotEmpty()) {
                item(key = "queued-title") {
                    CategoryExpenseDivider(
                        queuedSize = uiState.queued.size
                    )
                }
                items(uiState.queued.size, key = { "queued-$it" }) { index ->
                    ExpenseQueuedItem(
                        draft = uiState.queued[index],
                        number = index + 1,
                        participants = uiState.participants,
                        currencyCode = uiState.currencyCode,
                        enabled = !uiState.isSending,
                        onDelete = { onUiEvent(CreateExpenseUiEvent.RemoveQueuedClicked(index)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseDivider(
    queuedSize: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(
                Res.string.feature_expenses_queued,
                queuedSize
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            thickness = Dimens.Base.dividerThickness,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopBar(
    uiState: CreateExpenseUiState = CreateExpenseUiState(),
    onUiEvent: (CreateExpenseUiEvent) -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    focusManager: FocusManager
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        title = {
            Text(
                stringResource(Res.string.feature_expenses_add_multiple),
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.BackClicked) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        actions = {
            if (uiState.isSending) {
                CircularProgressIndicator(modifier = Modifier.padding(MaterialTheme.spacing.base))
            } else {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onUiEvent(CreateExpenseUiEvent.SendClicked)
                    },
                    enabled = uiState.canSend
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(Res.string.feature_expenses_send_all)
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun CreateExpenseScreenPreview() {
    SparrowTheme {
        CreateExpenseScreen(
            uiState = CreateExpenseUiState(
                boardId = "board",
                isLoading = false,
                participants = listOf(
                    ExpenseParticipantUi("one", "Chris", true),
                    ExpenseParticipantUi("two", "Alex"),
                    ExpenseParticipantUi("three", "Mia")
                ),
                draft = ExpenseDraftUi(
                    "Dinner at Bella Italia",
                    "48.50",
                    "one",
                    setOf("one", "two", "three"),
                    ExpenseCategoryUi.FOOD
                ),
                queued = listOf(
                    ExpenseDraftUi(
                        "Drinks",
                        "23.00",
                        "two",
                        setOf("one", "two", "three"),
                        ExpenseCategoryUi.FOOD
                    ),
                    ExpenseDraftUi(
                        "Taxi",
                        "15.00",
                        "one",
                        setOf("one", "two"),
                        ExpenseCategoryUi.TRANSPORT
                    )
                )
            ),
            onUiEvent = {}
        )
    }
}
