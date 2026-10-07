package com.cbgm.sparrow.feature.polls.di

import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.feature.polls.data.repository.PollComposerRepositoryImpl
import com.cbgm.sparrow.feature.polls.domain.repository.PollComposerRepository
import com.cbgm.sparrow.feature.polls.domain.usecase.ClearFinishedPollUseCase
import com.cbgm.sparrow.feature.polls.domain.usecase.FinishPollUseCase
import com.cbgm.sparrow.feature.polls.domain.usecase.ObserveFinishedPollUseCase
import com.cbgm.sparrow.feature.polls.presentation.create.CreatePollViewModel
import com.cbgm.sparrow.feature.polls.presentation.message.PollMessageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val pollsModule =
    module {
        single<PollComposerRepository> { PollComposerRepositoryImpl() }

        factory { FinishPollUseCase(repository = get()) }
        factory { ObserveFinishedPollUseCase(repository = get()) }
        factory { ClearFinishedPollUseCase(repository = get()) }

        viewModel { CreatePollViewModel(mediaFiles = get(), finishPoll = get()) }
        viewModel { parameters ->
            PollMessageViewModel(
                part = parameters.get<PollUi>()
            )
        }
    }
