package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationCountUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationsUseCase
import org.koin.dsl.module

val inviteModule =
    module {
        factory { AcceptInvitationUseCase(repository = get(), policy = get()) }
        factory { DeclineInvitationUseCase(repository = get()) }
        factory { MarkInvitationsViewedUseCase(repository = get()) }
        factory { ObserveInvitationsUseCase(repository = get(), policy = get()) }
        factory { DeleteDeclinedOutgoingInvitationUseCase(repository = get()) }

        factory {
            ObservePendingInvitationsUseCase(
                observeInvitations = get(),
                policy = get()
            )
        }
        factory { ObservePendingInvitationCountUseCase(observePendingInvitations = get()) }
        factory { ObserveInvitationsContextUseCase(observeInvitations = get()) }
    }
