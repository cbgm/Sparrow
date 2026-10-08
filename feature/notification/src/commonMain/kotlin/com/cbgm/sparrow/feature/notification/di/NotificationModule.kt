package com.cbgm.sparrow.feature.notification.di

import com.cbgm.sparrow.feature.notification.domain.model.AppVisibilityState
import com.cbgm.sparrow.feature.notification.domain.usecase.ObserveConversationNotificationEventsUseCase
import com.cbgm.sparrow.feature.notification.domain.usecase.RegisterPushTokenUseCase
import com.cbgm.sparrow.feature.notification.domain.usecase.ResolveNotificationConversationUseCase
import com.cbgm.sparrow.feature.notification.domain.usecase.SynchronizePendingMessagesUseCase
import com.cbgm.sparrow.feature.notification.presentation.ConversationNotificationCoordinator
import com.cbgm.sparrow.feature.notification.presentation.navigation.NotificationNavigationController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val notificationModule =
    module {
        singleOf(::AppVisibilityState)
        singleOf(::NotificationNavigationController)
        singleOf(::ObserveConversationNotificationEventsUseCase)
        singleOf(::ConversationNotificationCoordinator)
        singleOf(::ResolveNotificationConversationUseCase)
        singleOf(::RegisterPushTokenUseCase)
        singleOf(::SynchronizePendingMessagesUseCase)
    }
