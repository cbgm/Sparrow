package com.cbgm.securechat.notification.di

import androidx.work.WorkerParameters
import com.cbgm.securechat.notification.platform.AndroidNotificationRuntime
import com.cbgm.securechat.notification.platform.PlatformNotificationRuntime
import com.cbgm.securechat.notification.platform.SecureChatNotificationIntentHandler
import com.cbgm.securechat.notification.platform.SecureChatNotificationManager
import com.cbgm.securechat.notification.presentation.ConversationNotificationPresenter
import com.cbgm.securechat.notification.push.PendingMessageSyncScheduler
import com.cbgm.securechat.notification.push.PushTokenRegistrationScheduler
import com.cbgm.securechat.notification.work.PendingMessageSyncWorker
import com.cbgm.securechat.notification.work.PushTokenRegistrationWorker
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
