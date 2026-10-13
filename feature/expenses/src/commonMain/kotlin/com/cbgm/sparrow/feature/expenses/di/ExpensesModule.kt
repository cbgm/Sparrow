package com.cbgm.sparrow.feature.expenses.di

import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseBalancesUseCase
import com.cbgm.sparrow.feature.expenses.domain.usecase.CalculateExpenseSettlementsUseCase
import org.koin.dsl.module

val expensesModule =
    module {
        factory { CalculateExpenseBalancesUseCase() }
        factory { CalculateExpenseSettlementsUseCase() }
    }
