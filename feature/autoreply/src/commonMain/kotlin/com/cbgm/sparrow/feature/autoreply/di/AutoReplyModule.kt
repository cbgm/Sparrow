package com.cbgm.sparrow.feature.autoreply.di

import com.cbgm.sparrow.feature.autoreply.data.datasource.AutoReplyDataSource
import com.cbgm.sparrow.feature.autoreply.data.repository.AutoReplyRepositoryImpl
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ActivateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ClaimAutoReplyForContactUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.CreateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.DeactivateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.DeleteAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ObserveActiveAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ObserveAutoRepliesUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ReleaseAutoReplyRecipientUseCase
import com.cbgm.sparrow.feature.autoreply.domain.usecase.UpdateAutoReplyUseCase
import com.cbgm.sparrow.feature.autoreply.presentation.AutoReplyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val autoReplyModule =
    module {
        single {
            AutoReplyDataSource(dao = get())
        }

        single<AutoReplyRepository> {
            AutoReplyRepositoryImpl(dataSource = get())
        }

        factory { ObserveAutoRepliesUseCase(repository = get()) }
        factory { ObserveActiveAutoReplyUseCase(repository = get()) }
        factory { CreateAutoReplyUseCase(repository = get()) }
        factory { UpdateAutoReplyUseCase(repository = get()) }
        factory { DeleteAutoReplyUseCase(repository = get()) }
        factory { ActivateAutoReplyUseCase(repository = get()) }
        factory { DeactivateAutoReplyUseCase(repository = get()) }
        factory { ClaimAutoReplyForContactUseCase(repository = get()) }
        factory { ReleaseAutoReplyRecipientUseCase(repository = get()) }

        viewModel {
            AutoReplyViewModel(
                observeAutoReplies = get(),
                createAutoReply = get(),
                updateAutoReply = get(),
                deleteAutoReply = get(),
                activateAutoReply = get(),
                deactivateAutoReply = get()
            )
        }
    }
