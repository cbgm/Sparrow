package com.cbgm.sparrow.core.logging

import co.touchlab.kermit.Logger

internal actual fun logErrorToPlatform(tag: String, message: String, throwable: Throwable?) {
    Logger.withTag(tag).e(throwable = throwable, message = { message })
}
