package com.cbgm.sparrow.feature.ocr.di

import com.cbgm.sparrow.feature.ocr.device.IosTextRecognizer
import com.cbgm.sparrow.feature.ocr.device.TextRecognizer
import org.koin.dsl.module

actual val platformOcrModule =
    module {
        single<TextRecognizer> { IosTextRecognizer() }
    }
