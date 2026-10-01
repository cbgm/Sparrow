package com.cbgm.sparrow.feature.polls.di

import com.cbgm.sparrow.feature.polls.presentation.create.CreatePollViewModel
import com.cbgm.sparrow.feature.polls.presentation.message.PollMessageViewModel
import com.cbgm.sparrow.feature.polls.presentation.message.model.PollMessageUiState
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val pollsModule =
    module {
        viewModel { CreatePollViewModel(mediaFiles = get()) }
        viewModel { parameters ->
            PollMessageViewModel(
                initialState = parameters.get<PollMessageUiState>()
            )
        }
    }
