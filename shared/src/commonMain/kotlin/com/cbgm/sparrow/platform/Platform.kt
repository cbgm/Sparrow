package com.cbgm.sparrow.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
