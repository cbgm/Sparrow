package com.cbgm.sparrow.feature.transport.domain.port

/**
 * Starts the app's runtime when the process was started without the app being opened: the foreground service after
 * a reboot, or a check now and then. Starting again once it runs does nothing.
 */
interface HeadlessStartPort {
    suspend fun start()
}
