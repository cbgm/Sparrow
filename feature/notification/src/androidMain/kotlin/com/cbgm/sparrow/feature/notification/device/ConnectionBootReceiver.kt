package com.cbgm.sparrow.feature.notification.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cbgm.sparrow.core.util.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.feature.transport.domain.usecase.ObserveConnectionModeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** After a reboot or an app update, the chosen connection mode starts again without the app being opened. */
class ConnectionBootReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val observeConnectionMode by inject<ObserveConnectionModeUseCase>()
    private val applier by inject<ConnectionModeApplier>()
    private val applicationScope by inject<ApplicationCoroutineScope>()

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action !in RESTART_ACTIONS) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                applier.apply(observeConnectionMode().first())
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESTART_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
