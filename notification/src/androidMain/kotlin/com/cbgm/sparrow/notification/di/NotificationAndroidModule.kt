package com.cbgm.sparrow.notification.di

import androidx.work.WorkerParameters
import com.cbgm.sparrow.notification.platform.AndroidNotificationRuntime
import com.cbgm.sparrow.notification.platform.PlatformNotificationRuntime
import com.cbgm.sparrow.notification.platform.SparrowNotificationIntentHandler
import com.cbgm.sparrow.notification.platform.SparrowNotificationManager
import com.cbgm.sparrow.notification.presentation.ConversationNotificationPresenter
import com.cbgm.sparrow.notification.push.PendingMessageSyncScheduler
import com.cbgm.sparrow.notification.push.PushTokenRegistrationScheduler
import com.cbgm.sparrow.notification.work.PendingMessageSyncWorker
import com.cbgm.sparrow.notification.work.PushTokenRegistrationWorker
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
