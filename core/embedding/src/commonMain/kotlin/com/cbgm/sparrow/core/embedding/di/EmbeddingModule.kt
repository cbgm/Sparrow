package com.cbgm.sparrow.core.embedding.di

import com.cbgm.sparrow.core.embedding.data.repository.LocalEmbeddingRepositoryImpl
import com.cbgm.sparrow.core.embedding.data.storage.LocalEmbeddingSettingsStorage
import com.cbgm.sparrow.core.embedding.domain.repository.LocalEmbeddingRepository
import com.cbgm.sparrow.core.embedding.domain.usecase.InitializeLocalEmbeddingUseCase
import com.cbgm.sparrow.core.embedding.domain.usecase.ObserveLocalEmbeddingStateUseCase
import com.cbgm.sparrow.core.embedding.domain.usecase.SetLocalEmbeddingFeatureEnabledUseCase
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
