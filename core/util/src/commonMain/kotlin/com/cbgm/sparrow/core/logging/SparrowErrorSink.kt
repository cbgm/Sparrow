package com.cbgm.sparrow.core.logging

fun interface SparrowErrorSink {
    fun record(
        tag: String,
        timestampEpochMilliseconds: Long,
        message: String,
        throwable: Throwable?
    )
}
