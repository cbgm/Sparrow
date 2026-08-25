package com.cbgm.sparrow.notification.di

import androidx.work.WorkerParameters
import com.cbgm.sparrow.notification.device.AndroidNotificationRuntime
import com.cbgm.sparrow.notification.device.PendingMessageSyncScheduler
import com.cbgm.sparrow.notification.device.PendingMessageSyncWorker
import com.cbgm.sparrow.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.notification.device.PushTokenRegistrationScheduler
import com.cbgm.sparrow.notification.device.PushTokenRegistrationWorker
import com.cbgm.sparrow.notification.device.SparrowNotificationIntentHandler
import com.cbgm.sparrow.notification.device.SparrowNotificationManager
import com.cbgm.sparrow.notification.presentation.ConversationNotificationPresenter
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

        worker { parameters ->
            PendingMessageSyncWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                synchronizePendingMessages = get(),
                appVisibilityState = get(),
                conversationNotificationPresenter = get()
            )
        }

        worker { parameters ->
            PushTokenRegistrationWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                registerPushToken = get()
            )
        }
    }
