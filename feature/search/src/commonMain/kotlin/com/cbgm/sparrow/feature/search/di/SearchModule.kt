package com.cbgm.sparrow.feature.search.di

import com.cbgm.sparrow.feature.search.data.index.MessageSearchIndexer
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
        single { MessageSearchIndexer(dao = get(), embedder = get()) }
        single<SemanticSearchRepository> {
            SemanticSearchRepositoryImpl(
                localEmbeddingRepository = get(),
                indexer = get(),
                embedder = get(),
                dao = get(),
                applicationScope = get()
            )
        }
        single<MessageSearchRepository> {
            MessageSearchRepositoryImpl(
                dao = get(),
                semanticSearchRepository = get()
            )
        }
        factory { InitializeSemanticSearchUseCase(repository = get()) }
        factory { ObserveSemanticSearchStateUseCase(repository = get()) }
        factory { SetSemanticSearchEnabledUseCase(repository = get()) }
        factory { SearchMessagesUseCase(repository = get()) }
        viewModel {
            MessageSearchViewModel(
                observeSemanticSearchState = get(),
                searchMessages = get()
            )
        }
    }
