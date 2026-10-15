package com.cbgm.sparrow.feature.expenses.presentation.currency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_choose_currency
import com.cbgm.sparrow.resources.feature_expenses_choose_currency_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseCurrencyPickerDialog(
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ExpenseCurrencyPickerViewModel = koinViewModel()
) {
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    ExpenseCurrencyListDialog(
        title = stringResource(Res.string.feature_expenses_choose_currency),
        hint = stringResource(Res.string.feature_expenses_choose_currency_hint),
        currencies = currencies,
        onCurrencySelected = onCurrencySelected,
        onDismiss = onDismiss
    )
}
