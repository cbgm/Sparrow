package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.expenses.domain.ExpenseMoney
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_expenses_equal_split
import com.cbgm.sparrow.resources.feature_expenses_participants
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseSplitScreen(
    uiState: CreateExpenseUiState,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val splits = ExpenseMoney.parseEuroCents(uiState.draft.amount)?.let { amount ->
        runCatching { ExpenseMoney.splitEvenly(amount, uiState.draft.participantIds) }.getOrNull()
    }.orEmpty()
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
                title = { Text(stringResource(Res.string.feature_expenses_participants), style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.base_back))
                    }
                }
            )
        }
    ) { innerPadding, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.screenPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            item {
                Text(stringResource(Res.string.feature_expenses_equal_split), style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.participants.size) { index ->
                val person = uiState.participants[index]
                SparrowCardNoAnimation {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onUiEvent(CreateExpenseUiEvent.ParticipantToggled(person.id))
                        }.padding(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                    ) {
                        RoundCheckbox(
                            checked = person.id in uiState.draft.participantIds,
                            onCheckedChange = { onUiEvent(CreateExpenseUiEvent.ParticipantToggled(person.id)) }
                        )
                        SparrowAvatar(
                            name = person.displayName,
                            target = if (LocalInspectionMode.current) null else person.toAvatarTarget(),
                            size = Dimens.GroupConversationScreen.avatarSize
                        )
                        Text(person.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        splits[person.id]?.let { cents ->
                            Text(
                                "${cents / 100}.${(cents % 100).toString().padStart(2, '0')} ${uiState.currencyCode}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExpenseSplitScreenPreview() {
    com.cbgm.sparrow.core.ui.theme.SparrowTheme {
        ExpenseSplitScreen(
            uiState = CreateExpenseUiState(
                isLoading = false,
                draft = com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi(
                    "Dinner",
                    "48.50",
                    "a",
                    setOf("a", "b")
                ),
                participants = listOf(
                    com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi("a", "Chris", true),
                    com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi("b", "Alex")
                )
            ),
            onUiEvent = {}
        )
    }
}
