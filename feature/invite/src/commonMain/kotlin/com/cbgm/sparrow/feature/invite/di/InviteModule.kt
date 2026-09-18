package com.cbgm.sparrow.feature.invite.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.feature.invite.data.datasource.InvitationLifecycleDataSource
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleEffects
import com.cbgm.sparrow.feature.invite.data.outbox.InvitationOutboxDeliveryHandler
import com.cbgm.sparrow.feature.invite.data.policy.InvitationPolicyImpl
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPayloadEncoder
import com.cbgm.sparrow.feature.invite.data.protocol.handler.IncomingInvitationPacketHandler
import com.cbgm.sparrow.feature.invite.data.protocol.handler.InvitationAcceptedPacketHandler
import com.cbgm.sparrow.feature.invite.data.protocol.handler.InvitationDeclinedPacketHandler
import com.cbgm.sparrow.feature.invite.data.repository.InvitationRepositoryImpl
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadataProvider
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import com.cbgm.sparrow.feature.invite.domain.usecase.AcceptInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineAndBlockInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleIncomingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationsViewedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationResultsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsContextUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationCountUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ObservePendingInvitationsUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ValidatePendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.presentation.InvitationViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val inviteModule =
    module {
        singleOf(::InvitationPayloadEncoder)

        single<InvitationRepository> {
            val effectsByPayloadType =
                getAll<InvitationLifecycleEffects>().associateBy { effects -> effects.payloadType }
            val metadataProvidersByPayloadType =
                getAll<InvitationPeerMetadataProvider>().associateBy { provider -> provider.payloadType }
            val lifecycleDataSources =
                InvitationPayloadType.entries.map { payloadType ->
                    InvitationLifecycleDataSource(
                        invitationDao = get(),
                        effects =
                            requireNotNull(effectsByPayloadType[payloadType]) {
                                "No invitation lifecycle effects registered for $payloadType"
                            },
                        peerMetadataProvider =
                            requireNotNull(metadataProvidersByPayloadType[payloadType]) {
                                "No invitation peer metadata provider registered for $payloadType"
                            }
                    )
                }
            InvitationRepositoryImpl(lifecycleDataSources = lifecycleDataSources)
        }

        single<InvitationPolicy> {
            InvitationPolicyImpl(
                repository = get(),
                provider = get()
            )
        }
        factory { AcceptInvitationUseCase(repository = get(), policy = get()) }
        factory { DeclineInvitationUseCase(repository = get()) }
        factory { DeclineAndBlockInvitationUseCase(repository = get()) }
        factory { HandleIncomingInvitationUseCase(policy = get()) }
        factory { HandleInvitationResponseUseCase(repository = get()) }
        factory { RecordPendingInvitationUseCase(repository = get()) }
        factory { ShouldRecordPendingInvitationUseCase(repository = get()) }
        factory { ValidatePendingInvitationUseCase(repository = get()) }
        factory { MarkInvitationTransportFailedUseCase(repository = get()) }
        singleOf(::InvitationOutboxDeliveryHandler)
        factory { MarkInvitationsViewedUseCase(repository = get()) }
        factory { SendInvitationUseCase(repository = get()) }
        factory { ObserveInvitationsUseCase(repository = get(), policy = get()) }
        factory { ObserveInvitationLifecycleStatusUseCase(repository = get()) }
        factory { ObserveInvitationResultsUseCase(repository = get()) }
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
