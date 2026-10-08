package com.cbgm.sparrow.protocol.version

object ProtocolVersion {
    const val CURRENT: Int = 1

    fun isSupported(version: Int): Boolean = version == CURRENT
}
