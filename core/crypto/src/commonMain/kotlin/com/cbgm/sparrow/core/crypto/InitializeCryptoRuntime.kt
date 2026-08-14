package com.cbgm.sparrow.core.crypto

class InitializeCryptoRuntime {
    suspend operator fun invoke(): Result<Unit> = SodiumRuntime.initialize()
}
