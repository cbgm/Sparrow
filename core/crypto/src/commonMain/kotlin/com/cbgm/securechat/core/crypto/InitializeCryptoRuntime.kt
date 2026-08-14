package com.cbgm.securechat.core.crypto

class InitializeCryptoRuntime {
    suspend operator fun invoke(): Result<Unit> = SodiumRuntime.initialize()
}
