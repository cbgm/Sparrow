package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteAcceptedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteDeclinedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInvitePacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactReadyPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveIdentityHandshakeStateUseCase
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
            HandleContactInvitePacketUseCase(
                directIdentityExchangeRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { HandleContactInviteAcceptedPacketUseCase(directIdentityExchangeRepository = get()) }
        factory { HandleContactReadyPacketUseCase(directIdentityExchangeRepository = get()) }
        factory { HandleContactInviteDeclinedPacketUseCase(directIdentityExchangeRepository = get()) }

        factory {
            ObservePendingInvitationsUseCase(
                observeInvitations = get(),
                policy = get()
            )
        }
        factory { ObservePendingInvitationCountUseCase(observePendingInvitations = get()) }
        factory { ObserveIdentityHandshakeStateUseCase(directIdentityExchangeRepository = get()) }
        factory { ObserveInvitationsContextUseCase(observeInvitations = get()) }
    }
