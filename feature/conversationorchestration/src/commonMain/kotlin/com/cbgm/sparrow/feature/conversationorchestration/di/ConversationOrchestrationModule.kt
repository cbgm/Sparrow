package com.cbgm.sparrow.feature.conversationorchestration.di

import com.cbgm.sparrow.feature.conversationorchestration.data.direct.authorization.DirectAuthorizationPayloadEncoder
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectIdentityExchangeCoordinator
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectIdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationLifecycleDataSource
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationPacketProcessor
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation.DirectInvitationPolicyProvider
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.direct.DeleteDirectConversationWorkflowUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleDataSource
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val conversationOrchestrationModule =
    module {
        singleOf(::DirectAuthorizationPayloadEncoder)
        single {
            DirectIdentityExchangeCoordinator(
                invitationDao = get(),
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
        singleOf(::DirectInvitationLifecycleDataSource) {
            bind<InvitationLifecycleDataSource>()
        }
        singleOf(::DirectInvitationPacketProcessor) {
            bind<InvitationPacketProcessor>()
        }
        singleOf(::DirectInvitationPolicyProvider) {
            bind<InvitationPolicyProvider>()
        }

        singleOf(::InvitationResultObserver)
        singleOf(::DeleteDirectConversationWorkflowUseCase)
    }
