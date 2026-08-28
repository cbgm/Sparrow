package com.cbgm.sparrow.feature.search.di

import com.cbgm.sparrow.feature.search.data.datasource.MessageSearchIndexDataSource
import com.cbgm.sparrow.feature.search.data.datasource.MessageSearchLocalDataSource
import com.cbgm.sparrow.feature.search.data.datasource.SemanticSearchEmbeddingDataSource
import com.cbgm.sparrow.feature.search.data.repository.MessageSearchRepositoryImpl
import com.cbgm.sparrow.feature.search.data.repository.SemanticSearchRepositoryImpl
import com.cbgm.sparrow.feature.search.domain.repository.MessageSearchRepository
import com.cbgm.sparrow.feature.search.domain.repository.SemanticSearchRepository
import com.cbgm.sparrow.feature.search.domain.usecase.InitializeSemanticSearchUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.ObserveSemanticSearchStateUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SearchMessagesUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.SetSemanticSearchEnabledUseCase
import com.cbgm.sparrow.feature.search.presentation.overview.MessageSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule =
    module {
        single { MessageSearchLocalDataSource(dao = get()) }
        single { MessageSearchIndexDataSource(dao = get(), embedder = get()) }
        single { SemanticSearchEmbeddingDataSource(embedder = get()) }

        single<MessageSearchRepository> {
            MessageSearchRepositoryImpl(localDataSource = get())
        }
        single<SemanticSearchRepository> {
            SemanticSearchRepositoryImpl(
                indexDataSource = get(),
                localDataSource = get(),
                embeddingDataSource = get()
            )
        }

        factory {
            InitializeSemanticSearchUseCase(
                localEmbeddingRepository = get(),
                semanticSearchRepository = get(),
                applicationScope = get()
            )
        }
        factory {
            ObserveSemanticSearchStateUseCase(
                localEmbeddingRepository = get(),
                semanticSearchRepository = get()
            )
        }
        factory {
            SetSemanticSearchEnabledUseCase(
                localEmbeddingRepository = get(),
                semanticSearchRepository = get()
            )
        }
        factory {
            SearchMessagesUseCase(
                messageSearchRepository = get(),
                semanticSearchRepository = get()
            )
        }
        viewModel {
            MessageSearchViewModel(
                observeSemanticSearchState = get(),
                searchMessages = get()
            )
        }
    }
