package com.cbgm.sparrow.feature.expenses.presentation.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.component.SwipeActionTone
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.common.component.ExpenseMemberAvatar
import com.cbgm.sparrow.feature.expenses.presentation.common.component.label
import com.cbgm.sparrow.feature.expenses.presentation.create.icon
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.component.ExpenseOverviewSection
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseAmountUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseListItemUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseMemberBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseOverviewUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseOverviewUiState
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpensePaymentUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseSettlementUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseYourBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.overview.model.ExpenseYourPartUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_expenses_board
import com.cbgm.sparrow.resources.feature_expenses_delete
import com.cbgm.sparrow.resources.feature_expenses_delete_confirm
import com.cbgm.sparrow.resources.feature_expenses_delete_message
import com.cbgm.sparrow.resources.feature_expenses_delete_title
import com.cbgm.sparrow.resources.feature_expenses_edit
import com.cbgm.sparrow.resources.feature_expenses_overview_all_settled
import com.cbgm.sparrow.resources.feature_expenses_overview_balances
import com.cbgm.sparrow.resources.feature_expenses_overview_expenses
import com.cbgm.sparrow.resources.feature_expenses_overview_group_total
import com.cbgm.sparrow.resources.feature_expenses_overview_no_expenses
import com.cbgm.sparrow.resources.feature_expenses_overview_paid_on
import com.cbgm.sparrow.resources.feature_expenses_overview_pays
import com.cbgm.sparrow.resources.feature_expenses_overview_pays_you
import com.cbgm.sparrow.resources.feature_expenses_overview_people_owe_you
import com.cbgm.sparrow.resources.feature_expenses_overview_settle_hint
import com.cbgm.sparrow.resources.feature_expenses_overview_settle_up
import com.cbgm.sparrow.resources.feature_expenses_overview_settled
import com.cbgm.sparrow.resources.feature_expenses_overview_subtitle
import com.cbgm.sparrow.resources.feature_expenses_overview_transfers
import com.cbgm.sparrow.resources.feature_expenses_overview_you_borrowed
import com.cbgm.sparrow.resources.feature_expenses_overview_you_lent
import com.cbgm.sparrow.resources.feature_expenses_overview_you_owe_people
import com.cbgm.sparrow.resources.feature_expenses_overview_you_paid
import com.cbgm.sparrow.resources.feature_expenses_overview_you_paid_on
import com.cbgm.sparrow.resources.feature_expenses_overview_you_pay
import com.cbgm.sparrow.resources.feature_expenses_overview_your_balance
import com.cbgm.sparrow.resources.feature_expenses_overview_your_share
import com.cbgm.sparrow.resources.feature_payment_history
import com.cbgm.sparrow.resources.feature_payment_history_received
import com.cbgm.sparrow.resources.feature_payment_history_sent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseOverviewScreen(
    uiState: ExpenseOverviewUiState,
    onUiEvent: (ExpenseOverviewUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeletion by remember { mutableStateOf<ExpenseListItemUi?>(null) }
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            OverviewTopBar(
                uiState = uiState,
                containerColor = color,
                onBack = { onUiEvent(ExpenseOverviewUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, listState ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@SparrowLazyScaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.screenPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            item(key = "balance") { YourBalanceSection(uiState.yourBalance) }
            item(key = "settle") { SettleUpSection(uiState.settlements, onUiEvent) }
            if (uiState.payments.isNotEmpty()) {
                item(key = "payments") { PaymentsSection(uiState.payments, onUiEvent) }
            }
            item(key = "balances") { BalancesSection(uiState.balances) }
            item(key = "expenses") {
                ExpensesSection(
                    expenses = uiState.expenses,
                    onOpen = { expense ->
                        onUiEvent(
                            ExpenseOverviewUiEvent.ExpenseClicked(
                                expense.id
                            )
                        )
                    },
                    onEdit = { expense ->
                        onUiEvent(
                            ExpenseOverviewUiEvent.EditExpenseClicked(
                                expense.id
                            )
                        )
                    },
                    onDelete = { expense -> pendingDeletion = expense }
                )
            }
        }
    }

    DeleteExpenseDialog(
        expense = pendingDeletion,
        onDismiss = { pendingDeletion = null },
        onConfirm = { expense ->
            pendingDeletion = null
            onUiEvent(ExpenseOverviewUiEvent.DeleteExpenseConfirmed(expense.id))
        }
    )
}

@Composable
private fun DeleteExpenseDialog(
    expense: ExpenseListItemUi?,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseListItemUi) -> Unit
) {
    SparrowAlertDialog(
        isVisible = expense != null,
        onDismissRequest = onDismiss,
        confirmButton = {
            SparrowDialogAction(
                text = stringResource(Res.string.feature_expenses_delete_confirm),
                onClick = { expense?.let(onConfirm) },
                color = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = {
            SparrowDialogAction(
                text = stringResource(Res.string.base_cancel),
                onClick = onDismiss,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        title = stringResource(Res.string.feature_expenses_delete_title),
        text = {
            Text(
                stringResource(
                    Res.string.feature_expenses_delete_message,
                    expense?.description.orEmpty()
                )
            )
        }
    )
}

@Composable
private fun OverviewTopBar(
    uiState: ExpenseOverviewUiState,
    containerColor: Color,
    onBack: () -> Unit
) {
    SparrowTopBar(
        containerColor = containerColor,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    SparrowIcons.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.feature_expenses_board)
                )
                if (uiState.currencyCode.isNotBlank()) {
                    Text(
                        text = stringResource(
                            Res.string.feature_expenses_overview_subtitle,
                            uiState.currencyCode,
                            uiState.activatedAtLabel
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun YourBalanceSection(balance: ExpenseYourBalanceUi) {
    ExpenseOverviewSection(
        title = stringResource(Res.string.feature_expenses_overview_your_balance),
        collapsible = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = balance.balance.signedLabel(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = balance.balance.color()
            )
            Text(
                text = when (balance.balance) {
                    is ExpenseAmountUi.Owes -> pluralStringResource(
                        Res.plurals.feature_expenses_overview_you_owe_people,
                        balance.counterpartCount,
                        balance.counterpartCount
                    )

                    is ExpenseAmountUi.Owed -> pluralStringResource(
                        Res.plurals.feature_expenses_overview_people_owe_you,
                        balance.counterpartCount,
                        balance.counterpartCount
                    )

                    is ExpenseAmountUi.Settled -> stringResource(Res.string.feature_expenses_overview_settled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.divider,
                thickness = Dimens.Base.dividerThickness
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                BalanceStat(
                    stringResource(Res.string.feature_expenses_overview_group_total),
                    balance.totalLabel,
                    Modifier.weight(1f)
                )
                BalanceStat(
                    stringResource(Res.string.feature_expenses_overview_your_share),
                    balance.yourShareLabel,
                    Modifier.weight(1f)
                )
                BalanceStat(
                    stringResource(Res.string.feature_expenses_overview_you_paid),
                    balance.youPaidLabel,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BalanceStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettleUpSection(settlements: List<ExpenseSettlementUi>, onUiEvent: (ExpenseOverviewUiEvent) -> Unit) {
    ExpenseOverviewSection(
        title = stringResource(Res.string.feature_expenses_overview_settle_up),
        info = settlements.size.takeIf { it > 0 }?.let {
            pluralStringResource(Res.plurals.feature_expenses_overview_transfers, it, it)
        }
    ) {
        if (settlements.isEmpty()) {
            EmptyHint(stringResource(Res.string.feature_expenses_overview_all_settled))
        } else {
            Column {
                settlements.forEachIndexed { index, settlement ->
                    if (index > 0) RowDivider()
                    SettlementRow(settlement, onUiEvent)
                }
            }

            Text(
                text = stringResource(Res.string.feature_expenses_overview_settle_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettlementRow(settlement: ExpenseSettlementUi, onUiEvent: (ExpenseOverviewUiEvent) -> Unit) {
    val involvesYou = settlement.from.isLocal || settlement.to.isLocal
    val isClickable = settlement.canPay || (involvesYou && settlement.transferId != null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) {
                onUiEvent(ExpenseOverviewUiEvent.PaymentClicked(settlement.from.id, settlement.to.id, settlement.amountMinor, settlement.transferId))
            }
            .padding(
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        ExpenseMemberAvatar(if (settlement.from.isLocal) settlement.to else settlement.from)
        Text(
            text = when {
                settlement.from.isLocal -> stringResource(
                    Res.string.feature_expenses_overview_you_pay,
                    settlement.to.label()
                )

                settlement.to.isLocal -> stringResource(
                    Res.string.feature_expenses_overview_pays_you,
                    settlement.from.label()
                )

                else -> stringResource(
                    Res.string.feature_expenses_overview_pays,
                    settlement.from.label(),
                    settlement.to.label()
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = settlement.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isClickable) {
            Icon(
                imageVector = SparrowIcons.Chevron,
                contentDescription = null,
                modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PaymentsSection(payments: List<ExpensePaymentUi>, onUiEvent: (ExpenseOverviewUiEvent) -> Unit) {
    ExpenseOverviewSection(title = stringResource(Res.string.feature_payment_history)) {
        Column {
            payments.forEachIndexed { index, payment ->
                if (index > 0) RowDivider()
                Column(
                    Modifier.fillMaxWidth().clickable(enabled = payment.canOpen) {
                        onUiEvent(ExpenseOverviewUiEvent.PaymentClicked(payment.from.id, payment.to.id, payment.amountMinor, payment.transferId))
                    }.padding(vertical = MaterialTheme.spacing.base)
                ) {
                    Text("${payment.from.label()} → ${payment.to.label()} · ${payment.amountLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(if (payment.isReceived) Res.string.feature_payment_history_received else Res.string.feature_payment_history_sent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BalancesSection(balances: List<ExpenseMemberBalanceUi>) {
    ExpenseOverviewSection(title = stringResource(Res.string.feature_expenses_overview_balances)) {
        Column {
            balances.forEachIndexed { index, balance ->
                if (index > 0) RowDivider()
                MemberBalanceRow(balance)
            }
        }
    }
}

@Composable
private fun MemberBalanceRow(balance: ExpenseMemberBalanceUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        ExpenseMemberAvatar(balance.member)
        Text(
            text = balance.member.label(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        BalanceBar(balance, Modifier.weight(1f))
        Text(
            text = balance.balance.signedLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = balance.balance.color()
        )
    }
}

@Composable
private fun BalanceBar(
    balance: ExpenseMemberBalanceUi,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.height(Dimens.Expense.balanceBarHeight)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (balance.balance is ExpenseAmountUi.Owes) {
                Box(
                    Modifier
                        .fillMaxWidth(balance.fraction)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.shapes.extraSmall
                        )
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (balance.balance is ExpenseAmountUi.Owed) {
                Box(
                    Modifier
                        .fillMaxWidth(balance.fraction)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.extraSmall
                        )
                )
            }
        }
    }
}

@Composable
private fun ExpensesSection(
    expenses: List<ExpenseListItemUi>,
    onOpen: (ExpenseListItemUi) -> Unit,
    onEdit: (ExpenseListItemUi) -> Unit,
    onDelete: (ExpenseListItemUi) -> Unit
) {
    ExpenseOverviewSection(
        title = stringResource(Res.string.feature_expenses_overview_expenses),
        info = expenses.size.takeIf { it > 0 }?.toString()
    ) {
        if (expenses.isEmpty()) {
            EmptyHint(stringResource(Res.string.feature_expenses_overview_no_expenses))
        } else {
            Column {
                expenses.forEachIndexed { index, expense ->
                    if (index > 0) RowDivider()
                    SparrowSwipeRevealItem(
                        enabled = expense.canChange,
                        actions = if (!expense.canChange) {
                            emptyList()
                        } else {
                            listOf(
                                SwipeRevealAction(
                                    tone = SwipeActionTone.PRIMARY,
                                    onClick = { onEdit(expense) }
                                ) {
                                    Icon(
                                        SparrowIcons.Edit,
                                        contentDescription = stringResource(Res.string.feature_expenses_edit)
                                    )
                                },
                                SwipeRevealAction(
                                    tone = SwipeActionTone.DESTRUCTIVE,
                                    onClick = { onDelete(expense) }
                                ) {
                                    Icon(
                                        SparrowIcons.Delete,
                                        contentDescription = stringResource(Res.string.feature_expenses_delete)
                                    )
                                }
                            )
                        }
                    ) {
                        ExpenseRow(
                            expense = expense,
                            onClick = { onOpen(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: ExpenseListItemUi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                vertical = MaterialTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Icon(
            expense.category.icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
            tint = MaterialTheme.colorScheme.outline
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (expense.payer.isLocal) {
                    stringResource(
                        Res.string.feature_expenses_overview_you_paid_on,
                        expense.dateLabel
                    )
                } else {
                    stringResource(
                        Res.string.feature_expenses_overview_paid_on,
                        expense.payer.label(),
                        expense.dateLabel
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = expense.amountLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            when (val part = expense.yourPart) {
                is ExpenseYourPartUi.Lent -> Text(
                    text = stringResource(
                        Res.string.feature_expenses_overview_you_lent,
                        part.amountLabel
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                is ExpenseYourPartUi.Borrowed -> Text(
                    text = stringResource(
                        Res.string.feature_expenses_overview_you_borrowed,
                        part.amountLabel
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )

                ExpenseYourPartUi.NotInvolved -> Unit
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.divider,
        thickness = Dimens.Base.dividerThickness
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun ExpenseAmountUi.signedLabel(): String =
    when (this) {
        is ExpenseAmountUi.Owes -> "−$amountLabel"
        is ExpenseAmountUi.Owed -> "+$amountLabel"
        is ExpenseAmountUi.Settled -> amountLabel
    }

@Composable
private fun ExpenseAmountUi.color(): Color =
    when (this) {
        is ExpenseAmountUi.Owes -> MaterialTheme.colorScheme.error
        is ExpenseAmountUi.Owed -> MaterialTheme.colorScheme.tertiary
        is ExpenseAmountUi.Settled -> MaterialTheme.colorScheme.onSurface
    }

@Preview(heightDp = 1400)
@Composable
private fun ExpenseOverviewScreenPreview() {
    val you = ExpenseParticipantUi("you", "Chris", isLocal = true)
    val alex = ExpenseParticipantUi("alex", "Alex")
    val mia = ExpenseParticipantUi("mia", "Mia")
    SparrowTheme {
        ExpenseOverviewScreen(
            uiState = ExpenseOverviewUiState(
                isLoading = false,
                currencyCode = "EUR",
                activatedAtLabel = "12.06.26",
                yourBalance = ExpenseYourBalanceUi(
                    balance = ExpenseAmountUi.Owes("€168.80"),
                    totalLabel = "€1,086.50",
                    yourShareLabel = "€217.30",
                    youPaidLabel = "€48.50",
                    counterpartCount = 2
                ),
                settlements = listOf(
                    ExpenseSettlementUi(you, mia, "€132.70"),
                    ExpenseSettlementUi(you, alex, "€36.10")
                ),
                balances = listOf(
                    ExpenseMemberBalanceUi(alex, ExpenseAmountUi.Owed("€432.70"), 1f),
                    ExpenseMemberBalanceUi(mia, ExpenseAmountUi.Owed("€132.70"), 0.31f),
                    ExpenseMemberBalanceUi(you, ExpenseAmountUi.Owes("€168.80"), 0.39f)
                ),
                expenses = listOf(
                    ExpenseListItemUi(
                        "1",
                        "Dinner",
                        ExpenseCategoryUi.FOOD,
                        you,
                        "14.06.26",
                        "€48.50",
                        ExpenseYourPartUi.Lent("€38.80")
                    ),
                    ExpenseListItemUi(
                        "2",
                        "Hotel in Rome",
                        ExpenseCategoryUi.ACCOMMODATION,
                        alex,
                        "12.06.26",
                        "€650.00",
                        ExpenseYourPartUi.Borrowed("€130.00")
                    )
                )
            ),
            onUiEvent = {}
        )
    }
}
