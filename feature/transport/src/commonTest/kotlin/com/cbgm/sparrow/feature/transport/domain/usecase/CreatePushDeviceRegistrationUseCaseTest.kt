package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.push.PushPlatform
import com.cbgm.sparrow.feature.transport.push.pushDeviceRegistrationPayload
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CreatePushDeviceRegistrationUseCaseTest {
    private var signedPayload: ByteArray? = null
    private var signingPrivateKey: ByteArray? = null

    private val useCase =
        CreatePushDeviceRegistrationUseCase(
            signingKeyPairProvider =
                object : LocalSigningKeyPairProvider {
                    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                        Result.success(LocalSigningKeyPair(publicKey = PUBLIC_KEY, privateKey = PRIVATE_KEY))
                },
            signatureCrypto =
                object : DetachedSignatureCrypto {
                    override suspend fun sign(
                        payload: ByteArray,
                        signingPrivateKey: ByteArray
                    ): Result<ByteArray> {
                        signedPayload = payload
                        this@CreatePushDeviceRegistrationUseCaseTest.signingPrivateKey = signingPrivateKey
                        return Result.success(SIGNATURE)
                    }

                    override suspend fun verify(
                        payload: ByteArray,
                        signingPublicKey: ByteArray,
                        signature: ByteArray
                    ): Result<Unit> = Result.success(Unit)
                }
        )

    @Test
    fun payloadMatchesTheServerWireVector() {
        val payload =
            pushDeviceRegistrationPayload(
                routingId = "r",
                token = "t",
                platform = "p",
                issuedAtEpochMilliseconds = 258L
            )

        // The push server pins the same vector in ProtocolSignaturesTest.
        assertEquals(PUSH_DEVICE_VECTOR, payload.toHex())
    }

    @Test
    fun issueTimeIsEncodedAsEightByteBigEndian() {
        val payload =
            pushDeviceRegistrationPayload(
                routingId = "",
                token = "",
                platform = "",
                issuedAtEpochMilliseconds = 0x0102030405060708L
            )

        assertEquals("000000080102030405060708", payload.toHex().takeLast(24))
    }

    @Test
    fun signsRoutingIdTokenPlatformAndIssueTimeWithTheIdentityKey() =
        runTest {
            val request =
                useCase(
                    routingId = ROUTING_ID,
                    token = TOKEN,
                    platform = PushPlatform.ANDROID,
                    issuedAtEpochMilliseconds = ISSUED_AT
                ).getOrThrow()

            assertEquals(ROUTING_ID, request.routingId)
            assertEquals(TOKEN, request.token)
            assertEquals("ANDROID", request.platform)
            assertEquals(ISSUED_AT, request.issuedAtEpochMilliseconds)
            assertContentEquals(PUBLIC_KEY, request.clientSigningPublicKey)
            assertContentEquals(SIGNATURE, request.clientSignature)
            assertContentEquals(PRIVATE_KEY, signingPrivateKey)
            assertContentEquals(
                pushDeviceRegistrationPayload(
                    routingId = ROUTING_ID,
                    token = TOKEN,
                    platform = "ANDROID",
                    issuedAtEpochMilliseconds = ISSUED_AT
                ),
                signedPayload
            )
        }

    @Test
    fun blankTokenIsRefusedWithoutSigning() =
        runTest {
            val result = useCase(routingId = ROUTING_ID, token = " ", platform = PushPlatform.ANDROID)

            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertNull(signedPayload)
        }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        const val ROUTING_ID = "scrouting1_test"
        const val TOKEN = "fcm-token"
        const val ISSUED_AT = 1_700_000_000_000L
        const val PUSH_DEVICE_VECTOR =
            "0000001673706172726f772d707573682d6465766963652d7631" +
                "000000017200000001740000000170000000080000000000000102"
        val PUBLIC_KEY = byteArrayOf(1, 2, 3)
        val PRIVATE_KEY = byteArrayOf(4, 5, 6)
        val SIGNATURE = byteArrayOf(7, 8, 9)
    }
}
