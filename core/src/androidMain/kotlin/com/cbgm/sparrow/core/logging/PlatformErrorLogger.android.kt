package com.cbgm.sparrow.core.logging

import android.util.Log

internal actual fun logErrorToPlatform(tag: String, message: String, throwable: Throwable?) {
    // Keep Logcat reporting independent of Kermit's filtering and configuration.
    // Android host tests use an android.jar stub whose Log.e throws "not mocked";
    // that stub must not prevent a failure from reaching the snackbar, developer
    // log, or the code that is expected to handle the original failure.
    try {
        Log.e(tag, message, throwable)
    } catch (failure: RuntimeException) {
        if (failure.message?.contains("not mocked") != true) throw failure

        // JVM-host-test fallback only. Android devices still report through Logcat.
        System.err.println("ERROR/$tag: $message")
        throwable?.printStackTrace(System.err)
    }
}
