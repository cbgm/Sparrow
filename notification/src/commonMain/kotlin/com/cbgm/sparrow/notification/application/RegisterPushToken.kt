package com.cbgm.sparrow.notification.application

import com.cbgm.sparrow.feature.transport.push.PushPlatform
import com.cbgm.sparrow.feature.transport.push.PushTokenRegistrationGateway

class RegisterPushToken(
    private val pushTokenRegistrationGateway: PushTokenRegistrationGateway
) {
    suspend operator fun invoke(
        token: String,
        platform: PushPlatform
    ): Result<Unit> {
        require(token.isNotBlank()) {
            "Push token must not be blank"
        }

        return pushTokenRegistrationGateway.register(
            token = token,
            platform = platform
        )
    }
}
