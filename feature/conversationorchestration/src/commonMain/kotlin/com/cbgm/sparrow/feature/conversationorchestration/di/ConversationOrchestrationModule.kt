package com.cbgm.sparrow.feature.conversationorchestration.di

import com.cbgm.sparrow.feature.conversationorchestration.data.direct.authorization.DirectAuthorizationPayloadEncoder
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.identity.DirectIdentityExchangeCoordinator
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.identity.DirectIdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationLifecycleEffects
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationPacketProcessor
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationPeerMetadataProvider
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationPolicyProvider
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupInvitationLifecycleProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupInvitationPacketProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupInviteDeclinedIncomingProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupInviteIncomingProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupInviteReceivedIncomingProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation.GroupJoinRequestIncomingProcessorImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.outbox.InvitationOutboxDeliveryPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.OrchestratedOutboxDeliveryPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.DeletePeerConversationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationOpenUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.data.group.GroupInvitationLifecycleProcessor
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleEffects
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadataProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationOrchestrationModule =
    module {
        singleOf(::DirectAuthorizationPayloadEncoder)
        single {
            DirectIdentityExchangeCoordinator(
                identityExchangeDao = get(),
                contactDao = get(),
                contactRoutingIdDao = get(),
                contactKeyExchangeDataSource = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                secureRandomGenerator = get(),
                payloadEncoder = get(),
                authorizationPayloadEncoder = get(),
                protocolOutbox = get(),
                localPhoneNumberProvider = get(),
                phoneNumberNormalizer = get(),
                contactVerificationDataSource = get(),
                localProfilePictureMetadataProvider = get(),
                remoteProfilePictureMetadataProcessor = get()
            )
        }
        single<DirectIdentityExchangeRepository> {
            DirectIdentityExchangeRepositoryImpl(coordinator = get())
        }
        singleOf(::DirectInvitationLifecycleEffects) {
            bind<InvitationLifecycleEffects>()
        }
        singleOf(::DirectInvitationPeerMetadataProvider) {
            bind<InvitationPeerMetadataProvider>()
        }
        singleOf(::DirectInvitationPacketProcessor) {
            bind<InvitationPacketProcessor>()
        }
        singleOf(::DirectInvitationPolicyProvider) {
            bind<InvitationPolicyProvider>()
        }

        singleOf(::GroupInviteIncomingProcessorImpl)
        singleOf(::GroupInviteReceivedIncomingProcessorImpl)
        singleOf(::GroupInviteDeclinedIncomingProcessorImpl)
        singleOf(::GroupJoinRequestIncomingProcessorImpl)
        singleOf(::GroupInvitationPacketProcessorImpl) {
            bind<GroupInvitationPacketProcessor>()
        }
        singleOf(::GroupInvitationLifecycleProcessorImpl) {
            bind<GroupInvitationLifecycleProcessor>()
        }

        singleOf(::InvitationOutboxDeliveryPort) {
            bind<OrchestratedOutboxDeliveryPort>()
        }
        singleOf(::InvitationResultObserver)
        singleOf(::ObserveConversationQueueAvailabilityUseCase)
        singleOf(::PrepareConversationMessageUseCase)
        singleOf(::PrepareConversationOpenUseCase)
        singleOf(::AddConversationMembersUseCase)
        singleOf(::DeletePeerConversationUseCase)
    }
