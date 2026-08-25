package com.cbgm.sparrow.device

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
