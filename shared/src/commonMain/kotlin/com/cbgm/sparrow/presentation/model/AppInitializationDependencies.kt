package com.cbgm.sparrow.presentation.model

import com.cbgm.sparrow.core.crypto.InitializeCryptoRuntime
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.core.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.chats.data.direct.invitation.DirectInvitationConversationCoordinator
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.notification.presentation.ConversationNotificationCoordinator

data class AppInitializationDependencies(
    val initializeCryptoRuntime: InitializeCryptoRuntime,
    val platformNotificationRuntime: PlatformNotificationRuntime,
    val conversationNotificationCoordinator: ConversationNotificationCoordinator,
    val directInvitationConversationCoordinator: DirectInvitationConversationCoordinator,
    val controlPlaneConfiguration: ControlPlaneConfiguration,
    val controlPlaneStatusStore: ControlPlaneStatusStore,
    val controlPlaneDirectorySynchronizer: ControlPlaneDirectorySynchronizer,
    val controlPlaneHealthMonitor: ControlPlaneHealthMonitor,
    val observeLocalIdentityReady: ObserveLocalIdentityReadyUseCase,
    val importDeviceContacts: ImportDeviceContactsUseCase,
    val deviceContactsPermissionChecker: DeviceContactsPermissionRepository
)
