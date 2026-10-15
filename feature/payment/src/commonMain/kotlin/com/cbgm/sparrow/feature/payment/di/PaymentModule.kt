package com.cbgm.sparrow.feature.payment.di

import com.cbgm.sparrow.feature.payment.data.repository.BankDetailsRepositoryImpl
import com.cbgm.sparrow.feature.payment.data.repository.PaymentComposerRepositoryImpl
import com.cbgm.sparrow.feature.payment.domain.repository.BankDetailsRepository
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository
import com.cbgm.sparrow.feature.payment.domain.usecase.ClearPaymentContextUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.CompletePaymentRequestUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.DeleteBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.FindBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.ObservePaymentContextUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.ObservePaymentRequestsUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.ObservePaymentSessionUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PreparePaymentUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.PublishPaymentContextUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.RecordPaymentStatusUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.RecordSelectedPaymentUseCase
import com.cbgm.sparrow.feature.payment.domain.usecase.SaveBankDetailsUseCase
import com.cbgm.sparrow.feature.payment.presentation.PaymentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paymentModule = module {
    single<BankDetailsRepository> { BankDetailsRepositoryImpl(storage = get()) }
    factory { FindBankDetailsUseCase(repository = get()) }
    factory { SaveBankDetailsUseCase(repository = get()) }
    factory { DeleteBankDetailsUseCase(repository = get()) }
    single<PaymentComposerRepository> { PaymentComposerRepositoryImpl() }
    factory { ObservePaymentContextUseCase(repository = get()) }
    factory { PublishPaymentContextUseCase(repository = get()) }
    factory { ClearPaymentContextUseCase(repository = get()) }
    factory { ObservePaymentRequestsUseCase(repository = get()) }
    factory { CompletePaymentRequestUseCase(repository = get()) }
    factory { RecordPaymentStatusUseCase(repository = get()) }
    factory { ObservePaymentSessionUseCase(observeContext = get()) }
    factory {
        PreparePaymentUseCase(
            observeSession = get(),
            saveBankDetails = get(),
            deleteBankDetails = get()
        )
    }
    factory { RecordSelectedPaymentUseCase(observeSession = get(), recordStatus = get()) }
    viewModel {
        PaymentViewModel(
            savedStateHandle = get(),
            observeSession = get(),
            preparePayment = get(),
            recordPayment = get(),
            findBankDetails = get(),
            deleteBankDetails = get()
        )
    }
}
