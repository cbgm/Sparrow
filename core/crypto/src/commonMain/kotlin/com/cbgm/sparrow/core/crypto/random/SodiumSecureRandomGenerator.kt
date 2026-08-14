package com.cbgm.sparrow.core.crypto.random

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

class SodiumSecureRandomGenerator : SecureRandomGenerator {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun generateBytes(size: Int): Result<ByteArray> =
        runCatching {
            require(size > 0) {
                "Random byte count must be positive"
            }

            SodiumRuntime.initialize().getOrThrow()
            LibsodiumRandom.buf(size).toByteArray()
        }
}
