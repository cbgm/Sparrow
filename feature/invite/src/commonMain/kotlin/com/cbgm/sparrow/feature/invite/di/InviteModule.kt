package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.feature.invite.data.event.InvitationResultStreamImpl
import com.cbgm.sparrow.feature.invite.data.policy.InvitationPolicyImpl
import com.cbgm.sparrow.feature.invite.data.policy.InvitationPolicyProviderImpl
import com.cbgm.sparrow.feature.invite.data.protocol.handler.IncomingInvitationPacketHandler
import com.cbgm.sparrow.feature.invite.data.protocol.handler.InvitationAcceptedPacketHandler
import com.cbgm.sparrow.feature.invite.data.protocol.handler.InvitationDeclinedPacketHandler
import com.cbgm.sparrow.feature.invite.domain.event.InvitationResultStream
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleIncomingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationCountUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationsUseCase
import com.cbgm.sparrow.feature.invite.presentation.InvitationViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val inviteModule =
    module {
        single<InvitationPolicyProvider> {
            InvitationPolicyProviderImpl(
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        single<InvitationPolicy> {
            InvitationPolicyImpl(
                repository = get(),
                provider = get()
            )
        }
        single<InvitationResultStream> {
            InvitationResultStreamImpl(repository = get())
        }

        factory { AcceptInvitationUseCase(repository = get(), policy = get()) }
        factory { DeclineInvitationUseCase(repository = get()) }
        factory { DeclineAndBlockInvitationUseCase(repository = get()) }
        factory { HandleIncomingInvitationUseCase(policy = get()) }
        factory { HandleInvitationResponseUseCase(repository = get()) }
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

        viewModel {
            InvitationViewModel(
                savedStateHandle = get(),
                observeInvitationsContext = get(),
                acceptInvitation = get(),
                declineInvitation = get(),
                declineAndBlockInvitation = get(),
                deleteDeclinedOutgoingInvitation = get(),
                markInvitationsViewed = get()
            )
        }

        singleOf(::IncomingInvitationPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }
        singleOf(::InvitationAcceptedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }
        singleOf(::InvitationDeclinedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }
    }
