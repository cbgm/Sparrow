package com.cbgm.sparrow.feature.expenses.presentation.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCurrencyUi
import com.cbgm.sparrow.feature.expenses.presentation.currency.mapper.matching
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_expenses_search_currency
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpenseCurrencyListDialog(
    title: String,
    currencies: List<ExpenseCurrencyUi>,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    hint: String? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    SparrowAlertDialog(
        isVisible = true,
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            SparrowOutlinedButton(
                text = stringResource(Res.string.base_cancel),
                onClick = onDismiss,
                fillMaxWidth = false
            )
        },
        title = title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SparrowInputField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(Res.string.feature_expenses_search_currency),
                    placeholderText = "USD, $, …",
                    isSingleLine = true
                )
                val (pinned, others) = currencies.matching(query).partition { it.isPinned }
                LazyColumn(modifier = Modifier.height(Dimens.Expense.currencyListDialogHeight)) {
                    items(pinned, key = { it.code }) { currency ->
                        CurrencyItem(currency, onCurrencySelected)
                    }
                    if (pinned.isNotEmpty() && others.isNotEmpty()) {
                        item(key = "divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    items(others, key = { it.code }) { currency ->
                        CurrencyItem(currency, onCurrencySelected)
                    }
                }
            }
        }
    )
}

@Composable
private fun CurrencyItem(
    currency: ExpenseCurrencyUi,
    onCurrencySelected: (String) -> Unit
) {
    SparrowDialogListItem(
        text = currency.label,
        onClick = { onCurrencySelected(currency.code) },
        isEnabled = currency.isAvailable
    )
}
