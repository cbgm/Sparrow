package com.cbgm.sparrow.feature.expenses.presentation.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.base.messagepart.ui.model.ExpenseBoardClosedUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.ExpenseUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.MessagePartUi
import com.cbgm.sparrow.core.base.messagepart.ui.model.PaymentTransferUi
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.common.mapper.toExpenseAmountLabel
import com.cbgm.sparrow.feature.expenses.presentation.create.icon
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_failed
import com.cbgm.sparrow.resources.feature_chats_group_unknown_member
import com.cbgm.sparrow.resources.feature_expenses_activity_activated
import com.cbgm.sparrow.resources.feature_expenses_activity_added
import com.cbgm.sparrow.resources.feature_expenses_activity_closed
import com.cbgm.sparrow.resources.feature_expenses_activity_deleted
import com.cbgm.sparrow.resources.feature_expenses_activity_edited
import com.cbgm.sparrow.resources.feature_expenses_activity_participants
import com.cbgm.sparrow.resources.feature_expenses_activity_retry
import com.cbgm.sparrow.resources.feature_expenses_activity_you
import com.cbgm.sparrow.resources.feature_payment_activity_received
import com.cbgm.sparrow.resources.feature_payment_activity_sent
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseActivityMessage(
    part: MessagePartUi,
    senderName: String?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    isFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val actor = if (isMine) {
        stringResource(Res.string.feature_expenses_activity_you)
    } else {
        senderName?.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_unknown_member)
    }
    when (part) {
        is PaymentTransferUi -> ExpenseStatusMessage(
            text = stringResource(
                if (part.isReceiptConfirmed) Res.string.feature_payment_activity_received else Res.string.feature_payment_activity_sent,
                actor,
                part.amountMinor.toExpenseAmountLabel(part.currencyCode)
            ),
            isClosing = part.isReceiptConfirmed,
            modifier = modifier
        )
        is ExpenseBoardClosedUi -> ExpenseStatusMessage(
            text = stringResource(Res.string.feature_expenses_activity_closed, actor),
            isClosing = true,
            modifier = modifier
        )

        is ExpenseBoardUi -> ExpenseStatusMessage(
            text = stringResource(Res.string.feature_expenses_activity_activated, actor),
            isClosing = false,
            modifier = modifier
        )

        is ExpenseUi -> ExpenseAddedMessage(part, actor, modifier)
        else -> {}
    }
    if (isMine && isFailed) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.feature_chats_failed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(stringResource(Res.string.feature_expenses_activity_retry))
            }
        }
    }
}

@Composable
private fun ExpenseStatusMessage(text: String, isClosing: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = Alpha.GroupScreen.membershipSystemMessage),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.small
                ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isClosing) Icons.Default.CheckCircleOutline else Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ExpenseAddedMessage(expense: ExpenseUi, actor: String, modifier: Modifier = Modifier) {
    var expanded by remember(expense.id) { mutableStateOf(false) }
    val category = ExpenseCategoryUi.entries.firstOrNull { it.name == expense.category }
        ?: ExpenseCategoryUi.OTHER
    Surface(
        modifier = modifier.fillMaxWidth().clickable { expanded = !expanded },
        color = if (expense.deletedAtEpochMilliseconds == null) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when {
                                expense.deletedAtEpochMilliseconds != null -> Res.string.feature_expenses_activity_deleted
                                expense.editedAtEpochMilliseconds != null -> Res.string.feature_expenses_activity_edited
                                else -> Res.string.feature_expenses_activity_added
                            },
                            actor
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (expense.deletedAtEpochMilliseconds != null) TextDecoration.LineThrough else null,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    expense.amountMinor.toExpenseAmountLabel(expense.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (expense.deletedAtEpochMilliseconds != null) TextDecoration.LineThrough else null
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Text(
                    stringResource(
                        Res.string.feature_expenses_activity_participants,
                        expense.allocations.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExpenseActivityMessagePreview() {
    SparrowTheme {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
            ExpenseActivityMessage(
                part = ExpenseBoardUi("board", "EUR", 1L),
                senderName = "Alex",
                isMine = false
            )
            ExpenseActivityMessage(
                part = ExpenseUi(
                    id = "expense",
                    boardId = "board",
                    description = "Dinner at Bella Italia",
                    amountMinor = 4850L,
                    currencyCode = "EUR",
                    paidByMemberId = "member",
                    allocations = emptyList(),
                    occurredAtEpochMilliseconds = 1L,
                    category = "FOOD"
                ),
                senderName = "Alex",
                isMine = false
            )
            ExpenseActivityMessage(
                part = ExpenseBoardClosedUi("closed", "board", 2L),
                senderName = "Alex",
                isMine = false
            )
        }
    }
}
