package com.cbgm.sparrow.notification.di

import com.cbgm.sparrow.notification.application.AppVisibilityState
import com.cbgm.sparrow.notification.application.ConversationNotificationCoordinator
import com.cbgm.sparrow.notification.application.ObserveConversationNotificationEvents
import com.cbgm.sparrow.notification.application.RegisterPushToken
import com.cbgm.sparrow.notification.application.ResolveNotificationConversation
import com.cbgm.sparrow.notification.application.SynchronizePendingMessages
import com.cbgm.sparrow.notification.navigation.NotificationNavigationController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val notificationModule =
    module {
        singleOf(::AppVisibilityState)
        singleOf(::NotificationNavigationController)
        singleOf(::ObserveConversationNotificationEvents)
        singleOf(::ConversationNotificationCoordinator)
        singleOf(::ResolveNotificationConversation)
        singleOf(::RegisterPushToken)
        singleOf(::SynchronizePendingMessages)
    }
