package com.cbgm.sparrow.feature.search.di

import com.cbgm.sparrow.feature.search.data.platform.AndroidSemanticSearchModelManager
import com.cbgm.sparrow.feature.search.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.search.data.platform.MediaPipeLocalTextEmbedder
import com.cbgm.sparrow.feature.search.data.platform.SemanticSearchModelManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidSearchModule =
    module {
        single {
            AndroidSemanticSearchModelManager(context = androidContext())
        }
        single<SemanticSearchModelManager> { get<AndroidSemanticSearchModelManager>() }
        single<LocalTextEmbedder> {
            MediaPipeLocalTextEmbedder(
                context = androidContext(),
                modelManager = get()
            )
        }
    }
