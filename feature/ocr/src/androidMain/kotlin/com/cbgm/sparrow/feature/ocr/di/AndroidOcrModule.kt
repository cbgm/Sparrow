package com.cbgm.sparrow.feature.ocr.di

import com.cbgm.sparrow.feature.ocr.device.AndroidTextRecognizer
import com.cbgm.sparrow.feature.ocr.device.TextRecognizer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformOcrModule =
    module {
        single<TextRecognizer> { AndroidTextRecognizer(context = androidContext()) }
    }
