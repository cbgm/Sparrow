package com.cbgm.sparrow.di

import com.cbgm.sparrow.core.crypto.di.cryptoModule
import com.cbgm.sparrow.core.protocol.di.protocolModule
import com.cbgm.sparrow.core.ui.di.coreUiModule
import com.cbgm.sparrow.feature.chats.di.chatsModule
import com.cbgm.sparrow.feature.contactimport.di.contactImportModule
import com.cbgm.sparrow.feature.contacts.di.contactsModule
import com.cbgm.sparrow.feature.identity.di.identityModule
import com.cbgm.sparrow.feature.messaging.di.messagingModule
import com.cbgm.sparrow.feature.onboarding.di.onboardingModule
import com.cbgm.sparrow.feature.search.di.searchModule
import com.cbgm.sparrow.feature.settings.di.settingsModule
import com.cbgm.sparrow.feature.transport.di.transportModule
import com.cbgm.sparrow.navigation.di.navigationModule
import com.cbgm.sparrow.notification.di.notificationModule
import com.cbgm.sparrow.startup.di.startupModule
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
        searchModule,
        navigationModule,
        sharedModule
    )
