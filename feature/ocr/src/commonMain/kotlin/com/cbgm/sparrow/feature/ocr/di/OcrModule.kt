package com.cbgm.sparrow.feature.ocr.di

import com.cbgm.sparrow.feature.ocr.data.repository.ReceiptScanRepositoryImpl
import com.cbgm.sparrow.feature.ocr.domain.repository.ReceiptScanRepository
import com.cbgm.sparrow.feature.ocr.domain.usecase.ScanReceiptUseCase
import org.koin.dsl.module

val ocrModule =
    module {
        single<ReceiptScanRepository> { ReceiptScanRepositoryImpl(textRecognizer = get()) }
        factory { ScanReceiptUseCase(repository = get()) }
    }
