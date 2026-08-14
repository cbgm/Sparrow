package com.cbgm.sparrow.core.protocol.version

object ProtocolVersion {
    const val CURRENT: Int = 1

    fun isSupported(version: Int): Boolean = version == CURRENT
}
