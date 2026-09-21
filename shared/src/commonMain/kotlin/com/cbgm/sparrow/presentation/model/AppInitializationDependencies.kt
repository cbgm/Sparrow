package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.core.crypto.InitializeCryptoRuntime
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.core.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.ContactBlockObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.IdentityResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.InvitationResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MembershipResultObserver
import com.cbgm.sparrow.feature.conversationorchestration.runtime.MessagingTransportResultObserver
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.notification.presentation.ConversationNotificationCoordinator
import com.cbgm.sparrow.runtime.AttachmentConversationNameObserver

data class AppInitializationDependencies(
    val initializeCryptoRuntime: InitializeCryptoRuntime,
    val platformNotificationRuntime: PlatformNotificationRuntime,
    val conversationNotificationCoordinator: ConversationNotificationCoordinator,
    val attachmentConversationNameObserver: AttachmentConversationNameObserver,
    val invitationResultObserver: InvitationResultObserver,
    val membershipResultObserver: MembershipResultObserver,
    val messagingTransportResultObserver: MessagingTransportResultObserver,
    val directIdentityResultObserver: IdentityResultObserver,
    val contactBlockObserver: ContactBlockObserver,
    val controlPlaneConfiguration: ControlPlaneConfiguration,
    val controlPlaneStatusStore: ControlPlaneStatusStore,
    val controlPlaneDirectorySynchronizer: ControlPlaneDirectorySynchronizer,
    val controlPlaneHealthMonitor: ControlPlaneHealthMonitor,
    val observeLocalIdentityReady: ObserveLocalIdentityReadyUseCase,
    val importDeviceContacts: ImportDeviceContactsUseCase,
    val deviceContactsPermissionChecker: DeviceContactsPermissionRepository
)
