package com.cbgm.sparrow.core.logging

/** Writes the full error and its cause to the platform's error-level log. */
internal expect fun logErrorToPlatform(tag: String, message: String, throwable: Throwable?)
