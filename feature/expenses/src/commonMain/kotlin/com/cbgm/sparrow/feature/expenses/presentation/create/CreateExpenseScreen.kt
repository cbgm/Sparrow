package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_expenses_add
import com.cbgm.sparrow.resources.feature_expenses_amount
import com.cbgm.sparrow.resources.feature_expenses_description
import com.cbgm.sparrow.resources.feature_expenses_description_hint
import com.cbgm.sparrow.resources.feature_expenses_equal_split
import com.cbgm.sparrow.resources.feature_expenses_paid_by
import com.cbgm.sparrow.resources.feature_expenses_participants
import com.cbgm.sparrow.resources.feature_expenses_you
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateExpenseScreen(
    uiState: CreateExpenseUiState,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
                title = { Text(stringResource(Res.string.feature_expenses_add), style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.base_back))
                    }
                },
                actions = {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.padding(MaterialTheme.spacing.base))
                    } else {
                        IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.CreateClicked) }, enabled = uiState.canCreate) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.feature_expenses_add))
                        }
                    }
                }
            )
        }
    ) { innerPadding, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.screenPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "description") {
                SparrowCardNoAnimation {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        SparrowInputField(
                            value = uiState.description,
                            onValueChange = { onUiEvent(CreateExpenseUiEvent.DescriptionChanged(it)) },
                            label = stringResource(Res.string.feature_expenses_description),
                            placeholderText = stringResource(Res.string.feature_expenses_description_hint),
                            maxLines = 4,
                            isEnabled = !uiState.isLoading && !uiState.isSending
                        )
                    }
                }
            }
            item(key = "amount") {
                SparrowCardNoAnimation {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        SparrowInputField(
                            value = uiState.amount,
                            onValueChange = { onUiEvent(CreateExpenseUiEvent.AmountChanged(it)) },
                            label = stringResource(Res.string.feature_expenses_amount) + " (" + uiState.currencyCode + ")",
                            placeholderText = "0.00",
                            isSingleLine = true,
                            isEnabled = !uiState.isLoading && !uiState.isSending
                        )
                    }
                }
            }
            item(key = "payer") {
                SparrowCardNoAnimation {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        Text(stringResource(Res.string.feature_expenses_paid_by), style = MaterialTheme.typography.titleSmall)
                        uiState.participants.forEach { member ->
                            val label = member.label()
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !uiState.isSending) {
                                    onUiEvent(CreateExpenseUiEvent.PayerSelected(member.id))
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.payerId == member.id,
                                    onClick = { onUiEvent(CreateExpenseUiEvent.PayerSelected(member.id)) },
                                    enabled = !uiState.isSending
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item(key = "participants") {
                SparrowCardNoAnimation {
                    Column(Modifier.padding(MaterialTheme.spacing.medium)) {
                        Text(stringResource(Res.string.feature_expenses_participants), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(Res.string.feature_expenses_equal_split),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.participants.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !uiState.isSending) {
                                    onUiEvent(CreateExpenseUiEvent.ParticipantToggled(member.id))
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = member.id in uiState.selectedParticipantIds,
                                    onCheckedChange = { onUiEvent(CreateExpenseUiEvent.ParticipantToggled(member.id)) },
                                    enabled = !uiState.isSending
                                )
                                Text(member.label(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            uiState.errorMessage?.takeIf(String::isNotBlank)?.let { error ->
                item(key = "error") {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ExpenseParticipantUi.label(): String =
    if (isLocal && displayName.isBlank()) stringResource(Res.string.feature_expenses_you) else displayName

@Preview
@Composable
private fun CreateExpenseScreenPreview() {
    SparrowTheme {
        CreateExpenseScreen(
            uiState = CreateExpenseUiState(
                description = "Hotel in Rome",
                amount = "120.00",
                boardId = "board-1",
                payerId = "member-1",
                participants = listOf(
                    ExpenseParticipantUi("member-1", "Chris", true),
                    ExpenseParticipantUi("member-2", "Alex")
                ),
                selectedParticipantIds = setOf("member-1", "member-2"),
                isLoading = false,
                canCreate = true
            ),
            onUiEvent = {}
        )
    }
}
