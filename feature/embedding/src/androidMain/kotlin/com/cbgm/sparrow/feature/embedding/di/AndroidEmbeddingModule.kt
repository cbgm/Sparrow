package com.cbgm.sparrow.feature.embedding.di

import com.cbgm.sparrow.feature.embedding.data.platform.AndroidLocalEmbeddingModelFiles
import com.cbgm.sparrow.feature.embedding.data.platform.AndroidLocalEmbeddingModelManager
import com.cbgm.sparrow.feature.embedding.data.platform.LocalEmbeddingModelManager
import com.cbgm.sparrow.feature.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.embedding.data.platform.MediaPipeLocalTextEmbedder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidEmbeddingModule =
    module {
        single { AndroidLocalEmbeddingModelFiles(context = androidContext()) }
        single {
            AndroidLocalEmbeddingModelManager(
                context = androidContext(),
                modelFiles = get()
            )
        }
        single<LocalEmbeddingModelManager> { get<AndroidLocalEmbeddingModelManager>() }
        single<LocalTextEmbedder> {
            MediaPipeLocalTextEmbedder(
                context = androidContext(),
                modelManager = get()
            )
        }
    }
