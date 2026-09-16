package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptDirectInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockDirectInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteAcceptedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteDeclinedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInvitePacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactReadyPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveDirectInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveDirectInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingDirectInvitationCountUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingDirectInvitationsUseCase
import org.koin.dsl.module

val inviteModule =
    module {
        factory { AcceptInvitationUseCase(repository = get()) }
        factory { DeclineInvitationUseCase(repository = get()) }
        factory { MarkInvitationsViewedUseCase(repository = get()) }
        factory { ObserveInvitationsUseCase(repository = get()) }
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
            AcceptDirectInvitationUseCase(
                directIdentityExchangeRepository = get(),
                acceptInvitation = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            DeclineAndBlockDirectInvitationUseCase(
                directIdentityExchangeRepository = get(),
                declineInvitation = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            ObserveDirectInvitationsUseCase(
                observeInvitations = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            ObservePendingDirectInvitationsUseCase(
                observeDirectInvitations = get(),
                modeRepository = get()
            )
        }
        factory { ObservePendingDirectInvitationCountUseCase(observePendingDirectInvitations = get()) }
        factory { ObserveIdentityHandshakeStateUseCase(directIdentityExchangeRepository = get()) }
        factory { ObserveDirectInvitationsContextUseCase(observeDirectInvitations = get()) }
    }
