package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
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
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(CreateExpenseUiEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.base_back))
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
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                    Text(
                        text = stringResource(Res.string.feature_expenses_split_between),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.feature_expenses_split_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "mode") {
                SplitModeSelector(
                    selected = draft.splitMode,
                    onSelected = { onUiEvent(CreateExpenseUiEvent.SplitModeSelected(it)) }
                )
            }
            item(key = "participants") {
                SparrowCardNoAnimation {
                    Column {
                        uiState.participants.forEachIndexed { index, person ->
                            if (index > 0) {
                                HorizontalDivider(
                                    thickness = Dimens.Base.dividerThickness,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                            ParticipantRow(
                                person = person,
                                draft = draft,
                                shareMinor = allocations?.get(person.id),
                                currencyCode = draft.currencyCode,
                                onUiEvent = onUiEvent
                            )
                        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        SplitModeOption(
            icon = Icons.Default.Groups,
            title = stringResource(Res.string.feature_expenses_split_equal_title),
            description = stringResource(Res.string.feature_expenses_split_equal_description),
            selected = selected == ExpenseSplitModeUi.EQUAL,
            onClick = { onSelected(ExpenseSplitModeUi.EQUAL) },
            modifier = Modifier.weight(1f)
        )
        SplitModeOption(
            icon = Icons.Default.PieChart,
            title = stringResource(Res.string.feature_expenses_split_custom_title),
            description = stringResource(Res.string.feature_expenses_split_custom_description),
            selected = selected == ExpenseSplitModeUi.CUSTOM,
            onClick = { onSelected(ExpenseSplitModeUi.CUSTOM) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SplitModeOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (selected) 1.5.dp else Dimens.Base.borderStrokeWidth,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        RoundCheckbox(
            checked = checked,
            onCheckedChange = { onUiEvent(CreateExpenseUiEvent.ParticipantToggled(person.id)) }
        )
        SparrowAvatar(
            name = person.displayName,
            target = if (LocalInspectionMode.current) null else person.toAvatarTarget(),
            size = Dimens.Expense.categoryBadgeSize
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
                fontWeight = FontWeight.SemiBold
            )
        }
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
    SparrowCardNoAnimation {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.Expense.categoryBadgeSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(Res.plurals.feature_expenses_split_participant_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                status?.let { (text, color) ->
                    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun ExpenseDraftUi.isCustomSplitExceeded(): Boolean =
    splitMode == ExpenseSplitModeUi.CUSTOM && totalMinor?.let { customAssignedMinor > it } == true

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
