package com.cbgm.sparrow.feature.safety.di

import com.cbgm.sparrow.feature.safety.data.classifier.EmbeddingMessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.data.index.MessageSafetyIndexer
import com.cbgm.sparrow.feature.safety.data.repository.MessageSafetyRepositoryImpl
import com.cbgm.sparrow.feature.safety.domain.analyzer.MessageSafetyStructuralAnalyzer
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository
import com.cbgm.sparrow.feature.safety.domain.resolver.MessageSafetyRiskResolver
import com.cbgm.sparrow.feature.safety.domain.usecase.AnalyzeMessageSafetyUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.InitializeMessageSafetyUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyStateUseCase
import org.koin.dsl.module

val safetyModule =
    module {
        single { MessageSafetyStructuralAnalyzer() }
        single<MessageSafetyClassifier> { EmbeddingMessageSafetyClassifier(embedder = get()) }
        single { MessageSafetyRiskResolver() }
        factory {
            AnalyzeMessageSafetyUseCase(
                structuralAnalyzer = get(),
                classifier = get(),
                riskResolver = get()
            )
        }
        single {
            MessageSafetyIndexer(
                dao = get(),
                analyzeMessageSafety = get()
            )
        }
        single<MessageSafetyRepository> {
            MessageSafetyRepositoryImpl(
                dao = get(),
                indexer = get(),
                localEmbeddingRepository = get(),
                applicationScope = get()
            )
        }
        factory { InitializeMessageSafetyUseCase(repository = get()) }
        factory { ObserveMessageSafetyAssessmentsUseCase(repository = get()) }
        factory { ObserveMessageSafetyStateUseCase(repository = get()) }
    }
