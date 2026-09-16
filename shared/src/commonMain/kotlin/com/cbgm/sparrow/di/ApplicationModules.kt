package com.cbgm.sparrow.di

import com.cbgm.sparrow.core.crypto.di.cryptoModule
import com.cbgm.sparrow.core.embedding.di.embeddingModule
import com.cbgm.sparrow.core.protocol.di.protocolModule
import com.cbgm.sparrow.core.ui.di.coreUiModule
import com.cbgm.sparrow.feature.attachments.di.attachmentsModule
import com.cbgm.sparrow.feature.autoreply.di.autoReplyModule
import com.cbgm.sparrow.feature.avatar.di.avatarModule
import com.cbgm.sparrow.feature.chats.di.chatsModule
import com.cbgm.sparrow.feature.contactimport.di.contactImportModule
import com.cbgm.sparrow.feature.contacts.di.contactsModule
import com.cbgm.sparrow.feature.identity.di.identityModule
import com.cbgm.sparrow.feature.invite.di.inviteModule
import com.cbgm.sparrow.feature.linkpreview.di.linkPreviewModule
import com.cbgm.sparrow.feature.media.di.mediaModule
import com.cbgm.sparrow.feature.membership.di.membershipModule
import com.cbgm.sparrow.feature.messaging.di.messagingModule
import com.cbgm.sparrow.feature.onboarding.di.onboardingModule
import com.cbgm.sparrow.feature.safety.di.safetyModule
import com.cbgm.sparrow.feature.search.di.searchModule
import com.cbgm.sparrow.feature.settings.di.settingsModule
import com.cbgm.sparrow.feature.transport.di.transportModule
import com.cbgm.sparrow.feature.voice.di.voiceModule
import com.cbgm.sparrow.navigation.di.navigationModule
import com.cbgm.sparrow.notification.di.notificationModule
import com.cbgm.sparrow.startup.di.startupModule
import org.koin.core.module.Module

internal val commonApplicationModules: List<Module> =
    listOf(
        cryptoModule,
        embeddingModule,
        protocolModule,
        coreUiModule,
        identityModule,
        onboardingModule,
        contactsModule,
        inviteModule,
        safetyModule,
        attachmentsModule,
        autoReplyModule,
        avatarModule,
        mediaModule,
        voiceModule,
        chatsModule,
        membershipModule,
        transportModule,
        linkPreviewModule,
        messagingModule,
        notificationModule,
        contactImportModule,
        startupModule,
        settingsModule,
        searchModule,
        navigationModule,
        sharedModule
    )
