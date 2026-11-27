package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.route.relay.RelayDirectoryLoader
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import com.cbgm.sparrow.protocol.phone.PhoneVerificationSlot
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

class PhoneAttestationRemoteDataSourceTest {
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    private val directoryKey = ByteArray(32) { 9 }
    private val requestKey = ByteArray(32) { 5 }
    private val cipher = ReadableTestCipher()
    private val signatures = RecordingSignatures()
    private val requests = mutableListOf<String>()
    private var sentRequest: PhoneAttestationRequestDto? = null
    private var sentSlotRequest: PhoneSlotRequestDto? = null
    private var limitReached = false
    private var noVerifier = false
    private var relaysLoaded = 0

    @Test
    fun theTokenGoesSealedForTheCertifiedRequestKeyAndTheAttestationComesBackOpened() =
        runTest {
            val attestation = source().attest("firebase-token", SLOT, "scrouting1_device").getOrThrow()

            assertEquals(ATTESTATION, attestation)
            assertEquals(listOf("GET /v1/phone-attestations/keys", "POST /v1/phone-attestations"), requests)
            assertContentEquals(directoryKey, signatures.verifiedWith)
            assertTrue(signatures.verifiedPayload.decodeToString().startsWith("sparrow-phone-keys-v1\n{"))
            assertContentEquals(requestKey, cipher.sealedFor)
            assertEquals("sparrow-phone-attestation-request-v1", cipher.sealedContext)
            assertEquals("firebase-token", sentRequest?.token)
            assertEquals("scrouting1_device", sentRequest?.routingId)
            assertEquals(SLOT, sentRequest?.slot)
        }

    @Test
    fun aSlotIsReservedSealedWithProofOfWorkAfterTheRelaysAreKnown() =
        runTest {
            val slot = source().reserve()

            assertEquals(PhoneVerificationSlot.Reserved(SLOT, 99L, 1), slot)
            assertEquals(1, relaysLoaded)
            assertEquals(listOf("GET /v1/phone-attestations/keys", "POST /v1/phone-attestations/slots"), requests)
            assertEquals("sparrow-phone-slot-request-v1", cipher.sealedContext)
            assertEquals(36, sentSlotRequest?.stamp?.length)
        }

    @Test
    fun aFullDayIsReportedWithTheTimeOfTheNextDay() =
        runTest {
            limitReached = true

            assertEquals(PhoneVerificationSlot.DayFull(NEXT_DAY), source().reserve())
        }

    @Test
    fun aDirectoryWithoutPhoneVerificationOffersNoSlot() =
        runTest {
            noVerifier = true

            assertEquals(PhoneVerificationSlot.NotOffered, source().reserve())
        }

    @Test
    fun aCertificateTheDirectoryKeyDidNotSignSendsNoToken() =
        runTest {
            signatures.valid = false

            assertTrue(source().attest("firebase-token", SLOT, "scrouting1_device").isFailure)
            assertEquals(listOf("GET /v1/phone-attestations/keys"), requests)
        }

    @Test
    fun theVerifiersDailyMaximumIsReportedAsSuch() =
        runTest {
            limitReached = true

            val failure = source().attest("firebase-token", SLOT, "scrouting1_device").exceptionOrNull()

            assertIs<PhoneVerificationLimitReachedException>(failure)
        }

    @Test
    fun withoutAPinnedDirectoryKeyNothingIsSent() =
        runTest {
            assertTrue(source(pinned = null).attest("firebase-token", SLOT, "scrouting1_device").isFailure)
            assertEquals(emptyList(), requests)
        }

    private fun source(pinned: String? = base64.encode(DER_PREFIX + directoryKey)) =
        PhoneAttestationRemoteDataSource(
            controlPlaneHttpClient = client(),
            controlPlaneConfiguration = configuration(),
            pinnedDirectoryKey = { pinned },
            signatureCrypto = signatures,
            cipher = cipher,
            json = Json,
            work = MailboxCreationWorkSolver(Sha256(), bits = 0),
            relays =
                object : RelayDirectoryLoader {
                    override suspend fun ensureRelays() {
                        relaysLoaded++
                    }
                }
        )

    private fun client(): HttpClient =
        HttpClient(
            MockEngine { request ->
                requests += "${request.method.value} ${request.url.encodedPath}"
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                when {
                    noVerifier -> respond("""{"error":"Not found"}""", HttpStatusCode.NotFound, json)
                    request.method == HttpMethod.Get -> respond(certificate(), headers = json)
                    limitReached ->
                        respond(
                            """{"error":"PHONE_VERIFICATION_LIMIT","retryAtEpochMilliseconds":$NEXT_DAY}""",
                            HttpStatusCode.TooManyRequests,
                            json
                        )
                    request.url.encodedPath.endsWith("/slots") -> {
                        val sealed = Json.decodeFromString<SealedPhoneAttestationRequestDto>((request.body as TextContent).text)
                        val plaintext = base64.decode(sealed.sealedRequest).drop(1).toByteArray()
                        val sent = Json.decodeFromString<PhoneSlotRequestDto>(plaintext.decodeToString())
                        sentSlotRequest = sent
                        val slot = """{"expiresAtEpochMilliseconds":99,"resends":1,"slot":"$SLOT"}"""
                        val reply = byteArrayOf(base64.decode(sent.replyKey).first()) + slot.encodeToByteArray()
                        respond("""{"sealedSlot":"${base64.encode(reply)}"}""", headers = json)
                    }
                    else -> {
                        val sealed = Json.decodeFromString<SealedPhoneAttestationRequestDto>((request.body as TextContent).text)
                        val plaintext = base64.decode(sealed.sealedRequest).drop(1).toByteArray()
                        val sent = Json.decodeFromString<PhoneAttestationRequestDto>(plaintext.decodeToString())
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
        val keys = """{"attestationKey":"${base64.encode(DER_PREFIX + ByteArray(32) { 4 })}","protocol":"sparrow-phone-keys-v1","requestKey":"${base64.encode(requestKey)}"}"""
        return """{"payload":"${base64.encode(keys.encodeToByteArray())}","signature":"${base64.encode(ByteArray(64))}"}"""
    }

    private fun configuration(): ControlPlaneLocalDataSource = DirectoryConfiguration()

    private class DirectoryConfiguration : ControlPlaneLocalDataSource {
        override val endpoints = MutableStateFlow(emptyList<ControlPlaneEndpoint>())
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
        override val manualBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>("https://directory.test")

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = emptyList()

        override val statuses = MutableStateFlow(emptyList<ControlPlaneEndpointStatus>())

        override fun markActive(endpoint: ControlPlaneEndpoint) = Unit

        override fun markAvailable(endpoint: ControlPlaneEndpoint) = Unit

        override fun markUnreachable(endpoint: ControlPlaneEndpoint) = Unit

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

    /** Stands in for SHA-256: zero bits are needed, so any digest does. */
    private class Sha256 : CryptoHash {
        override fun sha256(input: ByteArray): ByteArray = ByteArray(32)
    }

    private companion object {
        const val SLOT = "slot-token"
        const val NEXT_DAY = 1_800_086_400_000L
        const val ATTESTATION = """{"keys":{},"payload":"p","signature":"s"}"""
        val DER_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)
    }
}
