package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSwitch
import com.cbgm.sparrow.core.ui.component.SparrowTabs
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiState
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_expenses_split_balanced
import com.cbgm.sparrow.resources.feature_expenses_split_between
import com.cbgm.sparrow.resources.feature_expenses_split_custom_description
import com.cbgm.sparrow.resources.feature_expenses_split_custom_summary
import com.cbgm.sparrow.resources.feature_expenses_split_custom_title
import com.cbgm.sparrow.resources.feature_expenses_split_done
import com.cbgm.sparrow.resources.feature_expenses_split_equal_description
import com.cbgm.sparrow.resources.feature_expenses_split_equal_summary
import com.cbgm.sparrow.resources.feature_expenses_split_equal_title
import com.cbgm.sparrow.resources.feature_expenses_split_exceeded
import com.cbgm.sparrow.resources.feature_expenses_split_participant_count
import com.cbgm.sparrow.resources.feature_expenses_split_remaining
import com.cbgm.sparrow.resources.feature_expenses_split_subtitle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseSplitScreen(
    uiState: CreateExpenseUiState,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val draft = uiState.draft
    val allocations = draft.allocationsMinor()
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            SparrowTopBar(
                containerColor = color,
                title = { Text(text = stringResource(Res.string.feature_expenses_split_between)) },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.BackClicked) }) {
                        Icon(SparrowIcons.ArrowBack, contentDescription = stringResource(Res.string.base_back))
                    }
                }
            )
        },
        bottomBar = { color ->
            Surface(color = color) {
                SparrowApprovalButton(
                    onClick = { onUiEvent(CreateExpenseUiEvent.SplitDoneClicked) },
                    text = stringResource(Res.string.feature_expenses_split_done),
                    enabled = allocations != null,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(MaterialTheme.spacing.screenPadding)
                )
            }
        }
    ) { innerPadding, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.screenPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            item(key = "header") {
                Text(
                    text = stringResource(Res.string.feature_expenses_split_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "mode") {
                SplitModeSelector(
                    selected = draft.splitMode,
                    onSelected = { onUiEvent(CreateExpenseUiEvent.SplitModeSelected(it)) }
                )
            }
            item(key = "participants") {
                Column {
                    uiState.participants.forEach { person ->
                        ParticipantRow(
                            person = person,
                            draft = draft,
                            shareMinor = allocations?.get(person.id),
                            currencyCode = draft.currencyCode,
                            onUiEvent = onUiEvent
                        )
                        HorizontalDivider(
                            thickness = Dimens.Base.dividerThickness,
                            color = MaterialTheme.colorScheme.divider
                        )
                    }
                }
            }
            item(key = "summary") {
                SplitSummary(draft = draft, currencyCode = draft.currencyCode)
            }
        }
    }
}

@Composable
private fun SplitModeSelector(
    selected: ExpenseSplitModeUi,
    onSelected: (ExpenseSplitModeUi) -> Unit
) {
    val modes = ExpenseSplitModeUi.entries
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        SparrowTabs(
            labels = modes.map { mode -> mode.title() },
            selectedIndex = modes.indexOf(selected),
            onSelect = { index -> onSelected(modes[index]) }
        )
        Text(
            text = selected.description(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpenseSplitModeUi.title(): String =
    stringResource(
        when (this) {
            ExpenseSplitModeUi.EQUAL -> Res.string.feature_expenses_split_equal_title
            ExpenseSplitModeUi.CUSTOM -> Res.string.feature_expenses_split_custom_title
        }
    )

@Composable
private fun ExpenseSplitModeUi.description(): String =
    stringResource(
        when (this) {
            ExpenseSplitModeUi.EQUAL -> Res.string.feature_expenses_split_equal_description
            ExpenseSplitModeUi.CUSTOM -> Res.string.feature_expenses_split_custom_description
        }
    )

@Composable
private fun ParticipantRow(
    person: ExpenseParticipantUi,
    draft: ExpenseDraftUi,
    shareMinor: Long?,
    currencyCode: String,
    onUiEvent: (CreateExpenseUiEvent) -> Unit
) {
    val checked = person.id in draft.participantIds
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUiEvent(CreateExpenseUiEvent.ParticipantToggled(person.id)) }
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        SparrowAvatar(
            name = person.displayName,
            target = if (LocalInspectionMode.current) null else person.toAvatarTarget(),
            size = Dimens.GroupConversationScreen.avatarSize
        )
        Text(person.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        when {
            draft.splitMode == ExpenseSplitModeUi.CUSTOM && checked -> SparrowInputField(
                value = draft.customAmounts[person.id].orEmpty(),
                onValueChange = { onUiEvent(CreateExpenseUiEvent.CustomAmountChanged(person.id, it)) },
                label = currencyCode,
                placeholderText = "0,00",
                isSingleLine = true,
                isError = draft.isCustomSplitExceeded(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.width(Dimens.Expense.customAmountFieldWidth)
            )
            draft.splitMode == ExpenseSplitModeUi.EQUAL && shareMinor != null -> Text(
                text = shareMinor.toExpenseAmountLabel(currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SparrowSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SplitSummary(
    draft: ExpenseDraftUi,
    currencyCode: String
) {
    val total = draft.totalMinor ?: 0L
    val count = draft.participantIds.size
    val (detail, status) = when (draft.splitMode) {
        ExpenseSplitModeUi.EQUAL -> {
            val each = draft.allocationsMinor()?.values?.maxOrNull() ?: 0L
            stringResource(
                Res.string.feature_expenses_split_equal_summary,
                total.toExpenseAmountLabel(currencyCode),
                each.toExpenseAmountLabel(currencyCode)
            ) to null
        }
        ExpenseSplitModeUi.CUSTOM -> {
            val assigned = draft.customAssignedMinor
            val difference = total - assigned
            val status = when {
                difference > 0L -> stringResource(
                    Res.string.feature_expenses_split_remaining,
                    difference.toExpenseAmountLabel(currencyCode)
                ) to MaterialTheme.colorScheme.onSurfaceVariant
                difference < 0L -> stringResource(
                    Res.string.feature_expenses_split_exceeded,
                    (-difference).toExpenseAmountLabel(currencyCode)
                ) to MaterialTheme.colorScheme.error
                else -> stringResource(Res.string.feature_expenses_split_balanced) to MaterialTheme.colorScheme.tertiary
            }
            stringResource(
                Res.string.feature_expenses_split_custom_summary,
                assigned.toExpenseAmountLabel(currencyCode),
                total.toExpenseAmountLabel(currencyCode)
            ) to status
        }
    }
    Column {
        Text(
            text = pluralStringResource(Res.plurals.feature_expenses_split_participant_count, count, count),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        status?.let { (text, color) ->
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

private fun ExpenseDraftUi.isCustomSplitExceeded(): Boolean =
    splitMode == ExpenseSplitModeUi.CUSTOM && totalMinor?.let { customAssignedMinor > it } == true

private val previewParticipants = listOf(
    ExpenseParticipantUi("a", "You", true),
    ExpenseParticipantUi("b", "Sarah"),
    ExpenseParticipantUi("c", "Lukas")
)

@Preview
@Composable
private fun ExpenseSplitScreenEqualPreview() {
    SparrowTheme {
        ExpenseSplitScreen(
            uiState = CreateExpenseUiState(
                isLoading = false,
                participants = previewParticipants,
                draft = ExpenseDraftUi("Dinner", "48,50", "a", setOf("a", "b", "c"))
            ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun ExpenseSplitScreenCustomPreview() {
    SparrowTheme {
        ExpenseSplitScreen(
            uiState = CreateExpenseUiState(
                isLoading = false,
                participants = previewParticipants,
                draft = ExpenseDraftUi(
                    description = "Dinner",
                    amount = "48,50",
                    payerId = "a",
                    participantIds = setOf("a", "b"),
                    splitMode = ExpenseSplitModeUi.CUSTOM,
                    customAmounts = mapOf("a" to "30", "b" to "12,50")
                )
            ),
            onUiEvent = {}
        )
    }
}
