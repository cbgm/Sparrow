package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.core.crypto.InitializeCryptoRuntime
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.runtime.UnpublishedGroupStateRunner
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.ApprovedIdentityReconnectionObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.ContactBlockObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.IdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime.MessagingTransportResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.membership.runtime.MembershipResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecoverInterruptedDirectSendsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.runtime.PendingAuthorizationCleanupRunner
import com.cbgm.sparrow.feature.embedding.domain.usecase.InitializeLocalEmbeddingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ResolveIdentityStatusUseCase
import com.cbgm.sparrow.feature.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.feature.notification.presentation.ConversationNotificationDispatcher
import com.cbgm.sparrow.feature.safety.domain.usecase.InitializeMessageSafetyUseCase
import com.cbgm.sparrow.feature.search.domain.usecase.InitializeSemanticSearchUseCase
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneConfigurationRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneDirectoryRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneStatusRepository
import com.cbgm.sparrow.runtime.AttachmentConversationNameObserver

data class AppInitializationDependencies(
    val initializeCryptoRuntime: InitializeCryptoRuntime,
    val resolveIdentityStatus: ResolveIdentityStatusUseCase,
    val initializeLocalEmbedding: InitializeLocalEmbeddingUseCase,
    val initializeSemanticSearch: InitializeSemanticSearchUseCase,
    val initializeMessageSafety: InitializeMessageSafetyUseCase,
    val platformNotificationRuntime: PlatformNotificationRuntime,
    val conversationNotificationDispatcher: ConversationNotificationDispatcher,
    val attachmentConversationNameObserver: AttachmentConversationNameObserver,
    val invitationResultObserver: InvitationResultObserver,
    val membershipResultObserver: MembershipResultObserver,
    val messagingTransportResultObserver: MessagingTransportResultObserver,
    val directIdentityResultObserver: IdentityResultObserver,
    val approvedIdentityReconnectionObserver: ApprovedIdentityReconnectionObserver,
    val contactBlockObserver: ContactBlockObserver,
    val pendingAuthorizationCleanupRunner: PendingAuthorizationCleanupRunner,
    val recoverInterruptedDirectSends: RecoverInterruptedDirectSendsUseCase,
    val unpublishedGroupStateRunner: UnpublishedGroupStateRunner,
    val workflowRegistry: WorkflowRegistry,
    val controlPlaneConfiguration: ControlPlaneConfigurationRepository,
    val controlPlaneStatus: ControlPlaneStatusRepository,
    val controlPlaneDirectorySynchronizer: ControlPlaneDirectoryRepository,
    val observeLocalIdentityReady: ObserveLocalIdentityReadyUseCase,
    val importDeviceContacts: ImportDeviceContactsUseCase,
    val deviceContactsPermissionChecker: DeviceContactsPermissionRepository
)
