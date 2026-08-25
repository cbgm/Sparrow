package com.cbgm.sparrow.feature.safety.di

import com.cbgm.sparrow.feature.safety.data.datasource.MessageSafetyEmbeddingDataSource
import com.cbgm.sparrow.feature.safety.data.datasource.MessageSafetyLocalDataSource
import com.cbgm.sparrow.feature.safety.data.repository.MessageSafetyAnalysisRepositoryImpl
import com.cbgm.sparrow.feature.safety.data.repository.MessageSafetyRepositoryImpl
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyAnalysisRepository
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository
import com.cbgm.sparrow.feature.safety.domain.usecase.AnalyzeMessageSafetyUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.InitializeMessageSafetyUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyStateUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ProcessMessageSafetyBatchUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.MessageSafetyDetailsViewModel
import com.cbgm.sparrow.feature.safety.util.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.util.MessageSafetyStructuralAnalyzer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val safetyModule =
    module {
        single { MessageSafetyLocalDataSource(dao = get()) }
        single { MessageSafetyStructuralAnalyzer() }
        single { MessageSafetyClassifier() }
        single { MessageSafetyEmbeddingDataSource(embedder = get()) }

        single<MessageSafetyAnalysisRepository> {
            MessageSafetyAnalysisRepositoryImpl(
                structuralAnalyzer = get(),
                embeddingDataSource = get(),
                classifier = get()
            )
        }
        single<MessageSafetyRepository> {
            MessageSafetyRepositoryImpl(
                localDataSource = get(),
                applicationScope = get()
            )
        }

        factory { AnalyzeMessageSafetyUseCase(repository = get()) }
        factory {
            ProcessMessageSafetyBatchUseCase(
                repository = get(),
                analyzeMessageSafety = get()
            )
        }
        single {
            InitializeMessageSafetyUseCase(
                repository = get(),
                localEmbeddingRepository = get(),
                processMessageSafetyBatch = get(),
                applicationScope = get()
            )
        }
        factory { ObserveMessageSafetyAssessmentsUseCase(repository = get()) }
        factory { ObserveMessageSafetyStateUseCase(repository = get()) }

        viewModel {
            MessageSafetyDetailsViewModel(
                savedStateHandle = get(),
                blockContact = get(),
                observeContactBlocklist = get()
            )
        }
    }
