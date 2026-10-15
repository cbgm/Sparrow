package com.cbgm.sparrow.feature.expenses.presentation.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowStatusBadge
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinBalanceUi
import com.cbgm.sparrow.feature.expenses.presentation.pin.model.ExpensePinUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_board
import com.cbgm.sparrow.resources.feature_expenses_pin_count
import com.cbgm.sparrow.resources.feature_expenses_pin_settled
import com.cbgm.sparrow.resources.feature_expenses_pin_total
import com.cbgm.sparrow.resources.feature_expenses_pin_you_are_owed
import com.cbgm.sparrow.resources.feature_expenses_pin_you_owe
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpensePinContent(
    pin: ExpensePinUi,
    modifier: Modifier = Modifier
) {
    val title = stringResource(Res.string.feature_expenses_board)
    val count = pluralStringResource(Res.plurals.feature_expenses_pin_count, pin.expenseCount, pin.expenseCount)

    Row(
        modifier = modifier.padding(
            start = MaterialTheme.spacing.small,
            end = MaterialTheme.spacing.base,
            top = MaterialTheme.spacing.base,
            bottom = MaterialTheme.spacing.base
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.Expense.pinIconTileSize)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$title \u00B7 $count".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.feature_expenses_pin_total, pin.totalLabel),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ExpensePinBalance(pin.balance)

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpensePinBalance(balance: ExpensePinBalanceUi) {
    when (balance) {
        is ExpensePinBalanceUi.Owes -> BalanceBadge(
            caption = stringResource(Res.string.feature_expenses_pin_you_owe),
            amount = balance.amountLabel,
            badge = { SparrowStatusBadge(it, Icons.AutoMirrored.Filled.CallMade, MaterialTheme.colorScheme.error) }
        )

        is ExpensePinBalanceUi.Owed -> BalanceBadge(
            caption = stringResource(Res.string.feature_expenses_pin_you_are_owed),
            amount = balance.amountLabel,
            badge = { SparrowStatusBadge(it, Icons.AutoMirrored.Filled.CallReceived, MaterialTheme.colorScheme.tertiary) }
        )

        ExpensePinBalanceUi.Settled -> SparrowStatusBadge(
            text = stringResource(Res.string.feature_expenses_pin_settled),
            icon = Icons.Default.Check,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ExpensePinBalanceUi.Unavailable -> Unit
    }
}

@Composable
private fun BalanceBadge(
    caption: String,
    amount: String,
    badge: @Composable (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro / 2)
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        badge(amount)
    }
}

@Preview
@Composable
private fun ExpensePinContentOwesPreview() {
    SparrowTheme {
        ExpensePinContent(
            pin = ExpensePinUi(
                expenseCount = 5,
                totalLabel = "€1,086.50",
                balance = ExpensePinBalanceUi.Owes("€168.80")
            )
        )
    }
}

@Preview
@Composable
private fun ExpensePinContentOwedPreview() {
    SparrowTheme {
        ExpensePinContent(
            pin = ExpensePinUi(
                expenseCount = 1,
                totalLabel = "€650.00",
                balance = ExpensePinBalanceUi.Owed("€432.70")
            )
        )
    }
}

@Preview
@Composable
private fun ExpensePinContentSettledPreview() {
    SparrowTheme {
        ExpensePinContent(
            pin = ExpensePinUi(
                expenseCount = 0,
                totalLabel = "€0.00",
                balance = ExpensePinBalanceUi.Settled
            )
        )
    }
}
