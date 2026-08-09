package com.cbgm.securechat.di

import android.content.ContentResolver
import com.cbgm.securechat.core.security.RegistryTrustRoot
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactWriter
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactsDataSource
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactWriter
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactsDataSource
import com.cbgm.securechat.feature.identity.data.storage.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PublicIdentityStorage
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.notification.presentation.ConversationNotificationPresenter
import com.cbgm.securechat.platform.notification.SecureChatNotificationIntentHandler
import com.cbgm.securechat.platform.notification.SecureChatNotificationManager
import com.cbgm.securechat.platform.runtime.ForegroundRuntimeController
import com.cbgm.securechat.platform.transport.AndroidControlPlaneConfiguration
import com.cbgm.securechat.provider.AndroidBuildInfoProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific dependency definitions.
 *
 * These classes require Android Context and cannot live in commonMain.
 */
val appModule =
    module {

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(
                context = androidContext()
            )
        }

        single<PublicIdentityStorage> {
            AndroidPublicIdentityStorage(
                context = androidContext()
            )
        }

        single<BuildInfoProvider> {
            AndroidBuildInfoProvider()
        }

        single<ContentResolver> {
            androidContext().contentResolver
        }

        single<DeviceContactsDataSource> {
            AndroidDeviceContactsDataSource(
                contentResolver = get()
            )
        }

        single<DeviceContactWriter> {
            AndroidDeviceContactWriter(
                context = androidContext()
            )
        }

        single {
            AndroidControlPlaneConfiguration(
                context = androidContext()
            )
        }

        single<ControlPlaneConfiguration> {
            get<AndroidControlPlaneConfiguration>()
        }

        single<ControlPlaneStatusStore> {
            get<AndroidControlPlaneConfiguration>()
        }

        single {
            RelayTransportConfig(
                trustedRegistryRootNodeId = RegistryTrustRoot.NODE_ID
            )
        }

        single {
            SecureChatNotificationManager(
                context = androidContext()
            )
        }

        single<ConversationNotificationPresenter> {
            get<SecureChatNotificationManager>()
        }

        single {
            SecureChatNotificationIntentHandler(
                notificationNavigationController = get()
            )
        }

        single {
            ForegroundRuntimeController(
                identityRepository = get(),
                phoneNumberStorage = get(),
                incomingRelayRunner = get(),
                relayConnectionManager = get(),
                outboxRunner = get(),
                appVisibilityState = get(),
                mailboxCoordinator = get()
            )
        }
    }
