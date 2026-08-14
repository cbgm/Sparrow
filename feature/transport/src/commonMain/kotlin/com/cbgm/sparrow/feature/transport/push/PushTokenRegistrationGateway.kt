package com.cbgm.sparrow.feature.transport.push

enum class PushPlatform {
    ANDROID,
    IOS
}

interface PushTokenRegistrationGateway {
    suspend fun register(
        token: String,
        platform: PushPlatform
    ): Result<Unit>
}
