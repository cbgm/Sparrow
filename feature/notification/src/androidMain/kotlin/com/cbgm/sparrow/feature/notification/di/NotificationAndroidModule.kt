package com.cbgm.sparrow.feature.notification.di

import androidx.work.WorkerParameters
import com.cbgm.sparrow.feature.notification.device.AndroidConversationNotificationPresenter
import com.cbgm.sparrow.feature.notification.device.AndroidNotificationRuntime
import com.cbgm.sparrow.feature.notification.device.ConnectionCheckWorker
import com.cbgm.sparrow.feature.notification.device.ConnectionModeApplier
import com.cbgm.sparrow.feature.notification.device.PlatformNotificationRuntime
import com.cbgm.sparrow.feature.notification.device.SparrowNotificationIntentHandler
import com.cbgm.sparrow.feature.notification.presentation.ConversationNotificationPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val notificationAndroidModule =
    module {
        single {
            AndroidConversationNotificationPresenter(
                context = androidContext()
            )
        }

        single<ConversationNotificationPresenter> {
            get<AndroidConversationNotificationPresenter>()
        }

        single {
            SparrowNotificationIntentHandler(
                notificationNavigationController = get()
            )
        }

        single {
            ConnectionModeApplier(
                context = androidContext()
            )
        }

        single<PlatformNotificationRuntime> {
            AndroidNotificationRuntime(
                notificationPresenter = get(),
                observeConnectionMode = get(),
                connectionModeApplier = get(),
                applicationScope = get()
            )
        }

        worker { parameters ->
            ConnectionCheckWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                keepAlive = get(),
                headlessStart = get()
            )
        }
    }
