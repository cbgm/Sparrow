package com.cbgm.sparrow.feature.expenses.presentation.create.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.expenses.domain.ExpenseCurrencies
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.icon
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
    SparrowCardNoAnimation {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_expenses_category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.Bottom
                ) {
                    CurrencySelection(
                        draft = draft,
                        currencies = currencies,
                        enabled = enabled,
                        onUiEvent = onUiEvent
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
                }
                PayerSelection(
                    draft = draft,
                    participants = participants,
                    enabled = enabled,
                    onUiEvent = onUiEvent,
                    modifier = Modifier.weight(1f)
                )
            }
            ConvertedAmount(draft = draft)
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
            if (canAddExpense) {
                SparrowApprovalButton(
                    text = stringResource(Res.string.feature_expenses_add_another),
                    onClick = { onUiEvent(CreateExpenseUiEvent.AddAnotherClicked) },
                    enabled = draft.amount.isNotEmpty() && draft.description.isNotEmpty()
                )
            }
        }
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
    onUiEvent: (CreateExpenseUiEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.Bottom) {
        Text(
            text = stringResource(Res.string.feature_expenses_currency),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Row(
            modifier = Modifier
                .clickable(enabled = enabled) { expanded = true }
                .padding(vertical = MaterialTheme.spacing.small / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ExpenseCurrencies.symbol(draft.currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                Dimens.Base.borderStrokeWidth,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.medium
            )
            .clickable(enabled) { onScanReceipt() }
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.medium
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        if (receipt == null) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            MediaThumbnail(
                media = receipt,
                modifier = Modifier
                    .size(RECEIPT_THUMBNAIL_SIZE)
                    .clip(MaterialTheme.shapes.small)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(Res.string.feature_expenses_receipt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    if (receipt == null) {
                        Res.string.feature_expenses_auto_import
                    } else {
                        Res.string.feature_expenses_receipt_attached
                    }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (receipt == null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            IconButton(
                onClick = { onUiEvent(CreateExpenseUiEvent.RemoveReceiptClicked) },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.feature_expenses_remove_receipt),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val RECEIPT_THUMBNAIL_SIZE = 32.dp

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                Dimens.Base.borderStrokeWidth,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.medium
            )
            .clickable(enabled) { onUiEvent(CreateExpenseUiEvent.SplitPickerClicked) }
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.medium
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(Res.string.feature_expenses_split_between),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = splitSummary,
                style = MaterialTheme.typography.bodyMedium
                // fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategorySelection(
    draft: ExpenseDraftUi,
    enabled: Boolean,
    onUiEvent: (CreateExpenseUiEvent) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
        items(ExpenseCategoryUi.entries.size) { index ->
            val category = ExpenseCategoryUi.entries[index]
            FilterChip(
                selected = category == draft.category,
                onClick = { onUiEvent(CreateExpenseUiEvent.CategorySelected(category)) },
                enabled = enabled,
                label = {
                    Text(
                        text = category.label(),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

            Text(
                text = stringResource(Res.string.feature_expenses_paid_by),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(vertical = MaterialTheme.spacing.small / 2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                val selected = participants.firstOrNull { it.id == draft.payerId }
                if (selected != null) {
                    SparrowAvatar(
                        name = selected.displayName,
                        target = if (LocalInspectionMode.current) null else selected.toAvatarTarget(),
                        size = Dimens.GroupConversationScreen.avatarSize
                    )
                }
                Text(
                    text = selected?.displayName.orEmpty(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null)
            }

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
