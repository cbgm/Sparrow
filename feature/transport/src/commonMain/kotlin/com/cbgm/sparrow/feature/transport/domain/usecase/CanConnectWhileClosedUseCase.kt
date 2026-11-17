package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.background.keepsConnectionWhileClosed

/** Whether this device can keep Sparrow's connection while the app is closed (Android) or not (iOS). */
class CanConnectWhileClosedUseCase {
    operator fun invoke(): Boolean = keepsConnectionWhileClosed()
}
