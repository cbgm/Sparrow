package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteAcceptedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInviteDeclinedPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactInvitePacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleContactReadyPacketUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkContactInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveContactInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveContactInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingContactInvitationCountUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingContactInvitationsUseCase
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
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { HandleContactInviteAcceptedPacketUseCase(identityInvitationRepository = get()) }
        factory { HandleContactReadyPacketUseCase(identityInvitationRepository = get()) }
        factory { HandleContactInviteDeclinedPacketUseCase(identityInvitationRepository = get()) }

        factory {
            AcceptContactInvitationUseCase(
                directInvitationRepository = get(),
                acceptInvitation = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { DeclineContactInvitationUseCase(declineInvitation = get()) }
        factory {
            DeclineAndBlockContactInvitationUseCase(
                directInvitationRepository = get(),
                declineInvitation = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { MarkContactInvitationsViewedUseCase(markInvitationsViewed = get()) }
        factory {
            ObserveContactInvitationsUseCase(
                observeInvitations = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            ObservePendingContactInvitationsUseCase(
                invitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { ObservePendingContactInvitationCountUseCase(observePendingContactInvitations = get()) }
        factory { ObserveIdentityHandshakeStateUseCase(identityInvitationRepository = get()) }
        factory { ObserveContactInvitationsContextUseCase(observeContactInvitations = get()) }
    }
