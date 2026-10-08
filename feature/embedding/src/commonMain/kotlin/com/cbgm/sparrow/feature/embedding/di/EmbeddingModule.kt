package com.cbgm.sparrow.feature.embedding.di

import com.cbgm.sparrow.feature.embedding.data.repository.LocalEmbeddingRepositoryImpl
import com.cbgm.sparrow.feature.embedding.data.storage.LocalEmbeddingSettingsStorage
import com.cbgm.sparrow.feature.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.feature.embedding.domain.usecase.InitializeLocalEmbeddingUseCase
import com.cbgm.sparrow.feature.embedding.domain.usecase.ObserveLocalEmbeddingStateUseCase
import com.cbgm.sparrow.feature.embedding.domain.usecase.SetLocalEmbeddingFeatureEnabledUseCase
import org.koin.dsl.module

val embeddingModule =
    module {
        single { LocalEmbeddingSettingsStorage(dataStore = get()) }
        single<LocalEmbeddingRepository> {
            LocalEmbeddingRepositoryImpl(
                settingsStorage = get(),
                modelManager = get(),
                embedder = get(),
                applicationScope = get()
            )
        }
        factory { InitializeLocalEmbeddingUseCase(repository = get()) }
        factory { ObserveLocalEmbeddingStateUseCase(repository = get()) }
        factory { SetLocalEmbeddingFeatureEnabledUseCase(repository = get()) }
    }
