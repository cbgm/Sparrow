package com.cbgm.sparrow.core.crypto.safety

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.model.PublicIdentityKeySet
import com.cbgm.sparrow.core.crypto.util.ByteArrays

class SafetyNumberGenerator(
    private val cryptoHash: CryptoHash
) {
    fun generate(
        firstIdentity: PublicIdentityKeySet,
        secondIdentity: PublicIdentityKeySet
    ): Result<SafetyNumber> =
        runCatching {
            val firstEncoded = encodeIdentity(identity = firstIdentity)

            val secondEncoded = encodeIdentity(identity = secondIdentity)

            val ordered =
                if (ByteArrays.compareUnsigned(first = firstEncoded, second = secondEncoded) <= 0) {
                    OrderedIdentities(
                        first = firstEncoded,
                        second = secondEncoded
                    )
                } else {
                    OrderedIdentities(first = secondEncoded, second = firstEncoded)
                }

            val input =
                ByteArrays.concatenate(
                    DOMAIN_SEPARATOR,
                    ordered.first,
                    ordered.second
                )

            val digest = cryptoHash.sha256(input = input)

            check(digest.size == SHA_256_SIZE_BYTES) {
                "Expected a 32-byte SHA-256 digest"
            }

            SafetyNumber(groups = digest.toFiveDigitGroups())
        }

    private fun encodeIdentity(identity: PublicIdentityKeySet): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.withLengthPrefix(value = identity.signingPublicKey),
            ByteArrays.withLengthPrefix(value = identity.encryptionPublicKey)
        )

    private fun ByteArray.toFiveDigitGroups(): List<String> {
        require(size % 2 == 0) {
            "Digest byte count must be even"
        }

        return indices
            .step(2)
            .map { index ->
                val high = this[index].toInt() and 0xFF
                val low = this[index + 1].toInt() and 0xFF

                val value = high shl 8 or low

                value.toString().padStart(length = 5, padChar = '0')
            }
    }

    private data class OrderedIdentities(
        val first: ByteArray,
        val second: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OrderedIdentities

            if (!first.contentEquals(other.first)) return false
            if (!second.contentEquals(other.second)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = first.contentHashCode()
            result = 31 * result + second.contentHashCode()
            return result
        }
    }

    private companion object {
        const val SHA_256_SIZE_BYTES = 32

        val DOMAIN_SEPARATOR = "Sparrow Safety Number v1".encodeToByteArray()
    }
}
