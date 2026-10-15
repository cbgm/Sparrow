package com.cbgm.sparrow.feature.payment.di

import com.cbgm.sparrow.feature.payment.device.BankDetailsStorage
import com.cbgm.sparrow.feature.payment.device.IosBankDetailsStorage
import org.koin.dsl.module

actual val platformPaymentModule = module {
    single<BankDetailsStorage> { IosBankDetailsStorage() }
}
