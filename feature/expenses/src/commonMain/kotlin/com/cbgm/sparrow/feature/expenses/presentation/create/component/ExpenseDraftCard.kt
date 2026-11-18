package com.cbgm.sparrow.feature.expenses.presentation.create.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowChip
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.component.SparrowSelectField
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.label
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.CreateExpenseUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCurrencyUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseSplitModeUi
import com.cbgm.sparrow.feature.expenses.presentation.currency.ExpenseCurrencyListDialog
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_add_another
import com.cbgm.sparrow.resources.feature_expenses_amount
import com.cbgm.sparrow.resources.feature_expenses_auto_import
import com.cbgm.sparrow.resources.feature_expenses_category
import com.cbgm.sparrow.resources.feature_expenses_converted_amount
import com.cbgm.sparrow.resources.feature_expenses_currency
import com.cbgm.sparrow.resources.feature_expenses_description
import com.cbgm.sparrow.resources.feature_expenses_description_hint
import com.cbgm.sparrow.resources.feature_expenses_paid_by
import com.cbgm.sparrow.resources.feature_expenses_rate_unavailable
import com.cbgm.sparrow.resources.feature_expenses_receipt
import com.cbgm.sparrow.resources.feature_expenses_receipt_attached
import com.cbgm.sparrow.resources.feature_expenses_remove_receipt
import com.cbgm.sparrow.resources.feature_expenses_split_all_members
import com.cbgm.sparrow.resources.feature_expenses_split_between
import com.cbgm.sparrow.resources.feature_expenses_split_custom_members
import com.cbgm.sparrow.resources.feature_expenses_split_selected_members
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpenseDraftCard(
    draft: ExpenseDraftUi,
    participants: List<ExpenseParticipantUi>,
    currencies: List<ExpenseCurrencyUi>,
    enabled: Boolean,
    canAddExpense: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    onScanReceipt: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        SparrowSectionLabel(text = stringResource(Res.string.feature_expenses_category))
        CategorySelection(
            draft = draft,
            enabled = enabled,
            onUiEvent = onUiEvent
        )

        SparrowInputField(
            value = draft.description,
            onValueChange = { onUiEvent(CreateExpenseUiEvent.DescriptionChanged(it)) },
            label = stringResource(Res.string.feature_expenses_description),
            placeholderText = stringResource(Res.string.feature_expenses_description_hint),
            isSingleLine = true,
            isEnabled = enabled
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            CurrencySelection(
                draft = draft,
                currencies = currencies,
                enabled = enabled,
                onUiEvent = onUiEvent,
                modifier = Modifier.width(Dimens.Expense.currencyFieldWidth)
            )
            SparrowInputField(
                value = draft.amount,
                onValueChange = { onUiEvent(CreateExpenseUiEvent.AmountChanged(it)) },
                label = stringResource(Res.string.feature_expenses_amount),
                modifier = Modifier.weight(1f),
                isSingleLine = true,
                placeholderText = "0.00",
                isEnabled = enabled
            )
            PayerSelection(
                draft = draft,
                participants = participants,
                enabled = enabled,
                onUiEvent = onUiEvent,
                modifier = Modifier.weight(1f)
            )
        }
        ConvertedAmount(draft = draft)
        Column {
            MemberSelection(
                draft = draft,
                participants = participants,
                enabled = enabled,
                onUiEvent = onUiEvent
            )
            ReceiptSelection(
                receipt = draft.receipt,
                enabled = enabled,
                onUiEvent = onUiEvent,
                onScanReceipt = onScanReceipt
            )
        }
        if (canAddExpense && (draft.amount.isNotEmpty() || draft.description.isNotEmpty())) {
            AddToListRow(
                enabled = draft.amount.isNotEmpty() && draft.description.isNotEmpty(),
                onClick = { onUiEvent(CreateExpenseUiEvent.AddAnotherClicked) }
            )
        }
    }
}

@Composable
private fun AddToListRow(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val shape = MaterialTheme.shapes.medium
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.Button.height)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = lineColor,
                    style = Stroke(
                        width = Dimens.Base.borderStrokeWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(Dimens.Base.dashLength.toPx(), Dimens.Base.dashGap.toPx())
                        )
                    )
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = SparrowIcons.Add,
            contentDescription = null,
            modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
            tint = contentColor
        )
        Text(
            text = stringResource(Res.string.feature_expenses_add_another),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
private fun ConvertedAmount(draft: ExpenseDraftUi) {
    val converted = draft.baseTotalMinor
    if (!draft.isConverted || (draft.rate != null && converted == null)) return
    Text(
        text = when {
            draft.rate == null || converted == null ->
                stringResource(Res.string.feature_expenses_rate_unavailable)
            else -> stringResource(
                Res.string.feature_expenses_converted_amount,
                converted.toExpenseAmountLabel(draft.baseCurrencyCode)
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (draft.rate == null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun CurrencySelection(
    draft: ExpenseDraftUi,
    currencies: List<ExpenseCurrencyUi>,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    SparrowSelectField(
        label = stringResource(Res.string.feature_expenses_currency),
        value = ExpenseCurrencies.symbol(draft.currencyCode),
        onClick = { expanded = true },
        modifier = modifier,
        isEnabled = enabled
    )
    if (expanded) {
        ExpenseCurrencyListDialog(
            title = stringResource(Res.string.feature_expenses_currency),
            currencies = currencies,
            onCurrencySelected = { code ->
                expanded = false
                onUiEvent(CreateExpenseUiEvent.CurrencySelected(code))
            },
            onDismiss = { expanded = false }
        )
    }
}

@Composable
private fun ReceiptSelection(
    receipt: VisualMediaSelectionUi?,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    onScanReceipt: () -> Unit
) {
    if (receipt == null) {
        SparrowDetailRow(
            icon = SparrowIcons.Receipt,
            title = stringResource(Res.string.feature_expenses_receipt),
            subtitle = stringResource(Res.string.feature_expenses_auto_import),
            onClick = onScanReceipt,
            enabled = enabled
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled) { onScanReceipt() }
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        MediaThumbnail(
            media = receipt,
            modifier = Modifier
                .size(Dimens.SettingsScreen.primaryIconSize)
                .clip(MaterialTheme.shapes.extraSmall)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.feature_expenses_receipt),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(Res.string.feature_expenses_receipt_attached),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = SparrowIcons.Close,
            contentDescription = stringResource(Res.string.feature_expenses_remove_receipt),
            modifier = Modifier
                .size(Dimens.SettingsScreen.secondaryIconSize)
                .clickable(enabled) { onUiEvent(CreateExpenseUiEvent.RemoveReceiptClicked) },
            tint = MaterialTheme.colorScheme.outline
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = Dimens.SettingsScreen.primaryIconSize + MaterialTheme.spacing.small),
        thickness = Dimens.Base.dividerThickness,
        color = MaterialTheme.colorScheme.divider
    )
}

@Composable
private fun MemberSelection(
    draft: ExpenseDraftUi,
    participants: List<ExpenseParticipantUi>,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit
) {
    val allMembersSelected = participants.isNotEmpty() &&
        draft.participantIds.size == participants.size
    val splitSummary = when {
        draft.splitMode == ExpenseSplitModeUi.CUSTOM -> stringResource(
            Res.string.feature_expenses_split_custom_members,
            draft.participantIds.size
        )
        allMembersSelected -> stringResource(
            Res.string.feature_expenses_split_all_members,
            draft.participantIds.size
        )
        else -> stringResource(
            Res.string.feature_expenses_split_selected_members,
            draft.participantIds.size
        )
    }
    SparrowDetailRow(
        icon = SparrowIcons.Group,
        title = stringResource(Res.string.feature_expenses_split_between),
        subtitle = splitSummary,
        onClick = { onUiEvent(CreateExpenseUiEvent.SplitPickerClicked) },
        enabled = enabled
    )
}

@Composable
private fun CategorySelection(
    draft: ExpenseDraftUi,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        ExpenseCategoryUi.entries.forEach { category ->
            SparrowChip(
                selected = category == draft.category,
                onClick = { onUiEvent(CreateExpenseUiEvent.CategorySelected(category)) },
                label = category.label(),
                enabled = enabled
            )
        }
    }
}

@Composable
private fun PayerSelection(
    draft: ExpenseDraftUi,
    participants: List<ExpenseParticipantUi>,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val anchorWidth = maxWidth

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom
        ) {
            var expanded by remember { mutableStateOf(false) }

            SparrowSelectField(
                label = stringResource(Res.string.feature_expenses_paid_by),
                value = participants.firstOrNull { it.id == draft.payerId }?.displayName.orEmpty(),
                onClick = { expanded = true },
                isEnabled = enabled
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.width(anchorWidth)
            ) {
                participants.forEach { person ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    MaterialTheme.spacing.base
                                )
                            ) {
                                SparrowAvatar(
                                    name = person.displayName,
                                    target = if (LocalInspectionMode.current) null else person.toAvatarTarget(),
                                    size = Dimens.GroupConversationScreen.avatarSize
                                )
                                Text(
                                    text = person.displayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onUiEvent(CreateExpenseUiEvent.PayerSelected(person.id))
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ExpenseDraftCardPreview() {
    SparrowTheme {
        ExpenseDraftCard(
            draft = ExpenseDraftUi(
                "Dinner at Bella Italia",
                "48.50",
                "a",
                setOf("a", "b"),
                ExpenseCategoryUi.FOOD
            ),
            participants = listOf(
                ExpenseParticipantUi("a", "Chris", true),
                ExpenseParticipantUi("b", "Alex")
            ),
            currencies = listOf(ExpenseCurrencyUi("EUR", "€", isAvailable = true)),
            enabled = true,
            onUiEvent = {},
            canAddExpense = true,
            onScanReceipt = {}
        )
    }
}
