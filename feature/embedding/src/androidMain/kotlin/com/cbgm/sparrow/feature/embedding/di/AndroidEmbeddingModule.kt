package com.cbgm.sparrow.feature.embedding.di

import com.cbgm.sparrow.feature.embedding.data.platform.AndroidEmbeddingModelLocalDataSource
import com.cbgm.sparrow.feature.embedding.data.platform.AndroidLocalEmbeddingModelFiles
import com.cbgm.sparrow.feature.embedding.data.platform.EmbeddingModelLocalDataSource
import com.cbgm.sparrow.feature.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.embedding.data.platform.MediaPipeLocalTextEmbedder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidEmbeddingModule =
    module {
        single { AndroidLocalEmbeddingModelFiles(context = androidContext()) }
        single {
            AndroidEmbeddingModelLocalDataSource(
                context = androidContext(),
                modelFiles = get()
            )
        }
        single<EmbeddingModelLocalDataSource> { get<AndroidEmbeddingModelLocalDataSource>() }
        single<LocalTextEmbedder> {
            MediaPipeLocalTextEmbedder(
                context = androidContext(),
                modelDataSource = get()
            )
        }
    }
