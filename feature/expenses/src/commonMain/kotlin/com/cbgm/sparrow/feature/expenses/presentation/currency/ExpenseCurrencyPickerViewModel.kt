package com.cbgm.sparrow.feature.expenses.presentation.currency

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExchangeRatesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.RefreshExchangeRatesUseCase
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCurrencyUi
import com.cbgm.sparrow.feature.expenses.presentation.currency.mapper.toBaseCurrencyUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseCurrencyPickerViewModel(
    observeExchangeRates: ObserveExchangeRatesUseCase,
    refreshExchangeRates: RefreshExchangeRatesUseCase
) : BaseViewModel() {
    val currencies: StateFlow<List<ExpenseCurrencyUi>> =
        observeExchangeRates()
            .map { rates -> rates.toBaseCurrencyUi() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null.toBaseCurrencyUi())

    init {
        viewModelScope.launch { refreshExchangeRates() }
    }
}
