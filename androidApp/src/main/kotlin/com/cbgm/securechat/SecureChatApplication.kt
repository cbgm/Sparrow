package com.cbgm.securechat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.cbgm.securechat.core.crypto.di.cryptoModule
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.di.protocolModule
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.securechat.core.transport.ControlPlaneHealthMonitor
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.core.ui.di.coreUiModule
import com.cbgm.securechat.data.database.di.androidDatabaseModule
import com.cbgm.securechat.di.appModule
import com.cbgm.securechat.di.sharedModule
import com.cbgm.securechat.feature.chats.di.androidChatsModule
import com.cbgm.securechat.feature.chats.di.chatsModule
import com.cbgm.securechat.feature.contactimport.di.contactImportModule
import com.cbgm.securechat.feature.contacts.di.contactsModule
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.identity.di.androidIdentityStorageModule
import com.cbgm.securechat.feature.identity.di.identityModule
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import com.cbgm.securechat.feature.messaging.di.messagingModule
import com.cbgm.securechat.feature.onboarding.di.onboardingModule
import com.cbgm.securechat.feature.settings.di.settingsModule
import com.cbgm.securechat.feature.transport.di.transportModule
import com.cbgm.securechat.notification.application.ConversationNotificationCoordinator
import com.cbgm.securechat.notification.di.notificationAndroidModule
import com.cbgm.securechat.notification.di.notificationModule
import com.cbgm.securechat.notification.push.PushTokenRegistrationScheduler
import com.cbgm.securechat.platform.notification.SecureChatNotificationManager
import com.cbgm.securechat.platform.runtime.ForegroundRuntimeController
import com.cbgm.securechat.startup.di.startupModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module

class SecureChatApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val logger = SecureChatLog.withTag("SecureChatApplication")

    private val applicationModules: List<Module> =
        listOf(
            cryptoModule,
            protocolModule,
            coreUiModule,
            androidDatabaseModule,
            androidIdentityStorageModule,
            identityModule,
            onboardingModule,
            contactsModule,
            androidChatsModule,
            chatsModule,
            transportModule,
            messagingModule,
            notificationModule,
            notificationAndroidModule,
            appModule,
            sharedModule,
            contactImportModule,
            startupModule,
            settingsModule
        )

    override fun onCreate() {
        super.onCreate()

        initializeCrypto()

        val koin = initializeDependencyInjection()

        initializeRuntimeServices(koin)
        launchStartupTasks(koin)
    }

    private fun initializeDependencyInjection(): Koin =
        startKoin {
            androidLogger()
            androidContext(this@SecureChatApplication)
            workManagerFactory()
            modules(applicationModules)
        }.koin

    private fun initializeRuntimeServices(koin: Koin) {
        koin.get<SecureChatNotificationManager>().createChannels()

        koin.get<ConversationNotificationCoordinator>().start()

        koin.get<ForegroundRuntimeController>().start()

        observeControlPlaneRegistrationTargets(koin)
        startControlPlaneDirectorySync(koin)
        startControlPlaneHealthMonitoring(koin)
    }

    private fun observeControlPlaneRegistrationTargets(koin: Koin) {
        applicationScope.launch {
            waitUntilLocalIdentityIsReady(koin)

            val configuration = koin.get<ControlPlaneConfiguration>()
            val statusStore = koin.get<ControlPlaneStatusStore>()
            combine(
                configuration.endpoints,
                statusStore.statuses
            ) { endpoints, statuses ->
                val reachabilityByUrl =
                    statuses.associate { status ->
                        status.endpoint.baseUrl to status.reachability
                    }
                endpoints
                    .filter { endpoint ->
                        reachabilityByUrl[endpoint.baseUrl] !=
                            ControlPlaneReachability.UNREACHABLE
                    }.map { endpoint -> endpoint.baseUrl }
                    .toSet()
            }.distinctUntilChanged()
                .collectLatest {
                    koin.get<PushTokenRegistrationScheduler>().enqueueCurrentToken()
                }
        }
    }

    private fun startControlPlaneDirectorySync(koin: Koin) {
        applicationScope.launch {
            while (isActive) {
                koin.get<ControlPlaneDirectorySynchronizer>().refresh()
                delay(CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS)
            }
        }
    }

    private fun startControlPlaneHealthMonitoring(koin: Koin) {
        applicationScope.launch {
            while (isActive) {
                koin.get<ControlPlaneHealthMonitor>().refresh()
                delay(CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS)
            }
        }
    }

    private fun launchStartupTasks(koin: Koin) {
        applicationScope.launch {
            waitUntilLocalIdentityIsReady(koin)

            syncDeviceContactsIfPermitted(koin)
        }
    }

    private suspend fun waitUntilLocalIdentityIsReady(
        koin: Koin
    ) {
        val identityRepository = koin.get<IdentityRepository>()

        val localPhoneNameStorage = koin.get<LocalPhoneNameStorage>()

        combine(
            identityRepository.observeIdentity(),
            localPhoneNameStorage.observePhoneNumber()
        ) { identity, phoneNumber ->
            identity != null && !phoneNumber.isNullOrBlank()
        }.first { isReady ->
            isReady
        }
    }

    private suspend fun syncDeviceContactsIfPermitted(
        koin: Koin
    ) {
        if (!hasReadContactsPermission()) {
            logger.info { "Device contact sync skipped: READ_CONTACTS is not granted" }

            return
        }

        koin
            .get<ImportDeviceContacts>()
            .invoke()
            .fold(
                onSuccess = {
                    logger.info { "Device contact sync completed" }
                },
                onFailure = { error ->
                    logger.error(error) { "Device contact sync failed" }
                }
            )
    }

    private fun hasReadContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    private fun initializeCrypto() {
        runBlocking {
            SodiumRuntime
                .initialize()
                .getOrElse { error ->
                    throw IllegalStateException(
                        "SecureChat could not initialize its cryptographic runtime",
                        error
                    )
                }
        }
    }

    private companion object {
        const val CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS = 300_000L
        const val CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS = 60_000L
    }
}
