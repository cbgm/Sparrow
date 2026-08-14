package com.cbgm.securechat.di

import com.cbgm.securechat.core.crypto.di.cryptoModule
import com.cbgm.securechat.core.protocol.di.protocolModule
import com.cbgm.securechat.core.ui.di.coreUiModule
import com.cbgm.securechat.feature.chats.di.chatsModule
import com.cbgm.securechat.feature.contactimport.di.contactImportModule
import com.cbgm.securechat.feature.contacts.di.contactsModule
import com.cbgm.securechat.feature.identity.di.identityModule
import com.cbgm.securechat.feature.messaging.di.messagingModule
import com.cbgm.securechat.feature.onboarding.di.onboardingModule
import com.cbgm.securechat.feature.settings.di.settingsModule
import com.cbgm.securechat.feature.transport.di.transportModule
import com.cbgm.securechat.navigation.di.navigationModule
import com.cbgm.securechat.notification.di.notificationModule
import com.cbgm.securechat.startup.di.startupModule
import org.koin.core.module.Module

internal val commonApplicationModules: List<Module> =
    listOf(
        cryptoModule,
        protocolModule,
        coreUiModule,
        identityModule,
        onboardingModule,
        contactsModule,
        chatsModule,
        transportModule,
        messagingModule,
        notificationModule,
        contactImportModule,
        startupModule,
        settingsModule,
        navigationModule,
        sharedModule
    )
