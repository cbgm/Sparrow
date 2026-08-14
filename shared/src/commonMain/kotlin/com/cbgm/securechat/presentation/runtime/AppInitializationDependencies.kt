package com.cbgm.securechat.presentation.runtime

import com.cbgm.securechat.core.crypto.InitializeCryptoRuntime
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.securechat.core.transport.ControlPlaneHealthMonitor
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.securechat.notification.application.ConversationNotificationCoordinator
import com.cbgm.securechat.notification.platform.PlatformNotificationRuntime

data class AppInitializationDependencies(
    val initializeCryptoRuntime: InitializeCryptoRuntime,
    val platformNotificationRuntime: PlatformNotificationRuntime,
    val conversationNotificationCoordinator: ConversationNotificationCoordinator,
    val controlPlaneConfiguration: ControlPlaneConfiguration,
    val controlPlaneStatusStore: ControlPlaneStatusStore,
    val controlPlaneDirectorySynchronizer: ControlPlaneDirectorySynchronizer,
    val controlPlaneHealthMonitor: ControlPlaneHealthMonitor,
    val observeLocalIdentityReady: ObserveLocalIdentityReadyUseCase,
    val importDeviceContacts: ImportDeviceContactsUseCase,
    val deviceContactsPermissionChecker: DeviceContactsPermissionRepository
)
