package com.cbgm.sparrow.feature.notification.device

import com.cbgm.sparrow.core.util.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveConnectionModeUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class AndroidNotificationRuntime(
    private val notificationManager: SparrowNotificationManager,
    private val observeConnectionMode: ObserveConnectionModeUseCase,
    private val connectionModeApplier: ConnectionModeApplier,
    private val applicationScope: ApplicationCoroutineScope
) : PlatformNotificationRuntime {
    override fun initialize() {
        notificationManager.createChannels()
        // The chosen way of receiving messages while Sparrow is closed follows every change of the setting.
        applicationScope.launch {
            observeConnectionMode().distinctUntilChanged().collect(connectionModeApplier::apply)
        }
    }
}
