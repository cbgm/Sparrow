package com.cbgm.sparrow.feature.notification.di

import androidx.work.WorkerParameters
import com.cbgm.sparrow.feature.notification.device.AndroidNotificationRuntime
import com.cbgm.sparrow.feature.notification.device.BackgroundDeliveryReceiptSender
import com.cbgm.sparrow.feature.notification.device.PendingMessageSyncScheduler
import com.cbgm.sparrow.feature.notification.device.PendingMessageSyncWorker
import com.cbgm.sparrow.feature.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.feature.notification.device.PushTokenRegistrationScheduler
import com.cbgm.sparrow.feature.notification.device.PushTokenRegistrationWorker
import com.cbgm.sparrow.feature.notification.device.PushTokenRenewalWorker
import com.cbgm.sparrow.feature.notification.device.SparrowNotificationIntentHandler
import com.cbgm.sparrow.feature.notification.device.SparrowNotificationManager
import com.cbgm.sparrow.feature.notification.presentation.ConversationNotificationPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val notificationAndroidModule =
    module {
        single {
            PendingMessageSyncScheduler(
                context = androidContext()
            )
        }

        single {
            PushTokenRegistrationScheduler(
                context = androidContext()
            )
        }

        single {
            SparrowNotificationManager(
                context = androidContext()
            )
        }

        single<ConversationNotificationPresenter> {
            get<SparrowNotificationManager>()
        }

        single {
            SparrowNotificationIntentHandler(
                notificationNavigationController = get()
            )
        }

        single<PlatformNotificationRuntime> {
            AndroidNotificationRuntime(
                notificationManager = get(),
                pushTokenRegistrationScheduler = get()
            )
        }

        single {
            BackgroundDeliveryReceiptSender(
                protocolOutbox = get(),
                transportConnectionManager = get(),
                outboxRunner = get(),
                appVisibilityState = get()
            )
        }

        worker { parameters ->
            PendingMessageSyncWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                synchronizePendingMessages = get(),
                prepareControlPlane = get(),
                appVisibilityState = get(),
                conversationNotificationPresenter = get(),
                backgroundDeliveryReceiptSender = get(),
                notificationManager = get()
            )
        }

        worker { parameters ->
            PushTokenRegistrationWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                registerPushToken = get(),
                prepareControlPlane = get()
            )
        }

        worker { parameters ->
            PushTokenRenewalWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                pushTokenRegistrationScheduler = get()
            )
        }
    }
