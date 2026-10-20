package com.cbgm.sparrow.feature.expenses.di

import com.cbgm.sparrow.feature.expenses.data.datasource.LocalExchangeRateDataSource
import com.cbgm.sparrow.feature.expenses.data.datasource.RemoteExchangeRateDataSource
import com.cbgm.sparrow.feature.expenses.data.repository.ExchangeRateRepositoryImpl
import com.cbgm.sparrow.feature.expenses.data.repository.ExpenseComposerRepositoryImpl
import com.cbgm.sparrow.feature.expenses.domain.repository.ExchangeRateRepository
import com.cbgm.sparrow.feature.expenses.domain.repository.ExpenseComposerRepository
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseSettlementsUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ClearExpenseGroupUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.GetExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExchangeRatesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseOverviewUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseRequestsUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.ObserveExpenseSummaryUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.PublishExpenseGroupUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.RefreshExchangeRatesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.SubmitExpenseRequestUseCase
import com.cbgm.sparrow.feature.expenses.presentation.create.CreateExpenseViewModel
import com.cbgm.sparrow.feature.expenses.presentation.currency.ExpenseCurrencyPickerViewModel
import com.cbgm.sparrow.feature.expenses.presentation.detail.ExpenseDetailViewModel
import com.cbgm.sparrow.feature.expenses.presentation.overview.ExpenseOverviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expensesModule =
    module {
        single<ExpenseComposerRepository> { ExpenseComposerRepositoryImpl() }
        single { LocalExchangeRateDataSource(dataStore = get()) }
        single { RemoteExchangeRateDataSource(httpClient = get()) }
        single<ExchangeRateRepository> {
            ExchangeRateRepositoryImpl(localDataSource = get(), remoteDataSource = get())
        }

        factory { CalculateExpenseBalancesUseCase() }
        factory { CalculateExpenseSettlementsUseCase() }
        factory { PublishExpenseGroupUseCase(repository = get()) }
        factory { ClearExpenseGroupUseCase(repository = get()) }
        factory { ObserveExpenseRequestsUseCase(repository = get()) }
        factory { SubmitExpenseRequestUseCase(repository = get()) }
        factory { ObserveExchangeRatesUseCase(repository = get()) }
        factory { RefreshExchangeRatesUseCase(repository = get()) }
        factory { GetExpenseCreationContextUseCase(repository = get()) }
        factory {
            ObserveExpenseSummaryUseCase(repository = get(), calculateBalances = get())
        }
        factory {
            ObserveExpenseOverviewUseCase(
                repository = get(),
                calculateBalances = get(),
                calculateSettlements = get()
            )
        }

        viewModel {
            ExpenseCurrencyPickerViewModel(
                observeExchangeRates = get(),
                refreshExchangeRates = get()
            )
        }
        viewModel {
            CreateExpenseViewModel(
                savedStateHandle = get(),
                getContext = get(),
                submitRequest = get(),
                scanReceipt = get(),
                observeExchangeRates = get(),
                refreshExchangeRates = get(),
                mediaFiles = get()
            )
        }
        viewModel {
            ExpenseOverviewViewModel(savedStateHandle = get(), observeOverview = get(), submitRequest = get())
        }
        viewModel {
            ExpenseDetailViewModel(savedStateHandle = get(), observeOverview = get(), submitRequest = get())
        }
    }
