package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PhoneAttestationRemoteSourceTest {
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    private val directoryKey = ByteArray(32) { 9 }
    private val requestKey = ByteArray(32) { 5 }
    private val cipher = ReadableTestCipher()
    private val signatures = RecordingSignatures()
    private val requests = mutableListOf<String>()
    private var sentRequest: PhoneAttestationRequest? = null
    private var limitReached = false

    @Test
    fun theTokenGoesSealedForTheCertifiedRequestKeyAndTheAttestationComesBackOpened() =
        runTest {
            val attestation = source().attest("firebase-token", "scrouting1_device").getOrThrow()

            assertEquals(ATTESTATION, attestation)
            assertEquals(listOf("GET /v1/phone-attestations/keys", "POST /v1/phone-attestations"), requests)
            assertContentEquals(directoryKey, signatures.verifiedWith)
            assertTrue(signatures.verifiedPayload.decodeToString().startsWith("sparrow-phone-keys-v1\n{"))
            assertContentEquals(requestKey, cipher.sealedFor)
            assertEquals("sparrow-phone-attestation-request-v1", cipher.sealedContext)
            assertEquals("firebase-token", sentRequest?.token)
            assertEquals("scrouting1_device", sentRequest?.routingId)
        }

    @Test
    fun aCertificateTheDirectoryKeyDidNotSignSendsNoToken() =
        runTest {
            signatures.valid = false

            assertTrue(source().attest("firebase-token", "scrouting1_device").isFailure)
            assertEquals(listOf("GET /v1/phone-attestations/keys"), requests)
        }

    @Test
    fun theVerifiersDailyMaximumIsReportedAsSuch() =
        runTest {
            limitReached = true

            val failure = source().attest("firebase-token", "scrouting1_device").exceptionOrNull()

            assertIs<PhoneVerificationLimitReachedException>(failure)
        }

    @Test
    fun withoutAPinnedDirectoryKeyNothingIsSent() =
        runTest {
            assertTrue(source(pinned = null).attest("firebase-token", "scrouting1_device").isFailure)
            assertEquals(emptyList(), requests)
        }

    private fun source(pinned: String? = base64.encode(DER_PREFIX + directoryKey)) =
        PhoneAttestationRemoteSource(
            controlPlaneHttpClient = client(),
            controlPlaneConfiguration = configuration(),
            pinnedDirectoryKey = { pinned },
            signatureCrypto = signatures,
            cipher = cipher,
            json = Json
        )

    private fun client(): HttpClient =
        HttpClient(
            MockEngine { request ->
                requests += "${request.method.value} ${request.url.encodedPath}"
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                when {
                    request.method == HttpMethod.Get -> respond(certificate(), headers = json)
                    limitReached -> respond("""{"error":"PHONE_VERIFICATION_LIMIT"}""", HttpStatusCode.TooManyRequests, json)
                    else -> {
                        val sealed = Json.decodeFromString<SealedPhoneAttestationRequest>((request.body as TextContent).text)
                        val plaintext = base64.decode(sealed.sealedRequest).drop(1).toByteArray()
                        val sent = Json.decodeFromString<PhoneAttestationRequest>(plaintext.decodeToString())
                        sentRequest = sent
                        val reply = byteArrayOf(base64.decode(sent.replyKey).first()) + ATTESTATION.encodeToByteArray()
                        respond("""{"sealedAttestation":"${base64.encode(reply)}"}""", headers = json)
                    }
                }
            }
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json) }
        }

    private fun certificate(): String {
        val keys = """{"attestationKey":"a","protocol":"sparrow-phone-keys-v1","requestKey":"${base64.encode(requestKey)}"}"""
        return """{"payload":"${base64.encode(keys.encodeToByteArray())}","signature":"${base64.encode(ByteArray(64))}"}"""
    }

    private fun configuration(): ControlPlaneConfiguration = DirectoryConfiguration()

    private class DirectoryConfiguration : ControlPlaneConfiguration {
        override val endpoints = MutableStateFlow(emptyList<ControlPlaneEndpoint>())
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
        override val manualBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>("https://directory.test")

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = emptyList()

        override fun markActive(endpoint: ControlPlaneEndpoint) = Unit

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = error("Not used")

        override suspend fun addManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun removeManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = error("Not used")

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = error("Not used")
    }

    private class RecordingSignatures : DetachedSignatureCrypto {
        var valid = true
        var verifiedWith = ByteArray(0)
        var verifiedPayload = ByteArray(0)

        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = error("Not used")

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> {
            verifiedWith = signingPublicKey
            verifiedPayload = payload
            return if (valid) Result.success(Unit) else Result.failure(IllegalStateException("bad signature"))
        }
    }

    /** Readable stand-in for libsodium: the first byte names the key it was sealed for. */
    private class ReadableTestCipher : SealedMessageCipher {
        var sealedFor = ByteArray(0)
        var sealedContext = ""

        override suspend fun seal(
            message: ByteArray,
            recipientPublicKey: ByteArray,
            context: String
        ): Result<ByteArray> {
            sealedFor = recipientPublicKey
            sealedContext = context
            return Result.success(byteArrayOf(recipientPublicKey.first()) + message)
        }

        override suspend fun openReply(
            sealed: ByteArray,
            replyKey: ByteArray
        ): ByteArray? = sealed.takeIf { it.firstOrNull() == replyKey.first() }?.copyOfRange(1, sealed.size)

        override suspend fun newReplyKey(): ByteArray = ByteArray(32) { 7 }
    }

    private companion object {
        const val ATTESTATION = """{"keys":{},"payload":"p","signature":"s"}"""
        val DER_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)
    }
}
