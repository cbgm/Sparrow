package com.cbgm.sparrow.feature.linkpreview.di

import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.linkpreview.data.datasource.LocalLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.datasource.RemoteLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.repository.LinkPreviewRepositoryImpl
import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.GetLinkPreviewUseCase
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.PrefetchLinkPreviewsUseCase
import com.cbgm.sparrow.feature.linkpreview.presentation.LinkPreviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val linkPreviewModule =
    module {
        single {
            RemoteLinkPreviewDataSource(
                httpClient = get(),
                transportDiagnosticsProvider = get<TransportDiagnosticsProvider>()
            )
        }

        single {
            LocalLinkPreviewDataSource(
                linkPreviewDao = get()
            )
        }

        single<LinkPreviewRepository> {
            LinkPreviewRepositoryImpl(
                remoteLinkPreviewDataSource = get(),
                localLinkPreviewDataSource = get(),
                applicationScope = get()
            )
        }

        factory {
            GetLinkPreviewUseCase(repository = get())
        }

        factory {
            PrefetchLinkPreviewsUseCase(
                repository = get()
            )
        }

        viewModel { parameters ->
            LinkPreviewViewModel(
                url = parameters.get(),
                getLinkPreview = get()
            )
        }
    }
