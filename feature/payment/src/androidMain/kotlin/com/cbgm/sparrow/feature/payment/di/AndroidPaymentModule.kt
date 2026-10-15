package com.cbgm.sparrow.feature.payment.di

import com.cbgm.sparrow.feature.payment.device.AndroidBankDetailsStorage
import com.cbgm.sparrow.feature.payment.device.BankDetailsStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformPaymentModule = module {
    single<BankDetailsStorage>(createdAtStart = true) { AndroidBankDetailsStorage(androidContext()) }
}
