package com.cbgm.sparrow.core.logging

import android.util.Log

internal actual fun logErrorToPlatform(tag: String, message: String, throwable: Throwable?) {
    // Explicit Log.e ensures that error reporting does not depend on Kermit's
    // log-writer configuration or minimum-severity filtering.
    Log.e(tag, message, throwable)
}
