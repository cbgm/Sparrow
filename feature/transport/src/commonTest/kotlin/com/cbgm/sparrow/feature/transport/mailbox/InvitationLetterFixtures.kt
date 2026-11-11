package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.DefaultTransportPayloadCodec
import com.cbgm.sparrow.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.presence.InvitationRoute
import com.cbgm.sparrow.feature.transport.presence.InvitationRouteLookup
import com.cbgm.sparrow.feature.transport.presence.InvitationRouteRemoteSource
import com.cbgm.sparrow.feature.transport.presence.InvitationRouteResult
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Devices, presence directory and invitation mailboxes for tests, with readable stand-ins for libsodium: a device's
 * keys are its name, a "signature" is the signer's key with the payload's hash, and "sealing" puts the recipient's key
 * in front.
 */
internal class InvitationTestWorld {
    private val json = createGatewayJson()

    /** What the presence directory answers, by phone alias or device routing ID. */
    val presence = mutableMapOf<String, InvitationRouteResult>()
    val mailboxes = RecordingInvitationGateway()
    var presenceFails = false

    fun device(name: String): Device = Device(name)

    /** Publishes the invitation mailbox of [device] under its routing ID and its phone alias. */
    fun publish(device: Device) {
        val result = InvitationRouteResult(device.routingId, InvitationRoute(device.route, device.key))
        presence[device.routingId] = result
        presence[device.alias] = result
    }

    fun letters(device: Device): InvitationLetters =
        SignedInvitationLetters(
            routes = InvitationRouteRemoteSource(presenceClient(), Configuration, MailboxCreationWorkSolver(ZeroHash, bits = 0), json),
            mailboxGateway = mailboxes,
            signingKeyPairProvider = SigningKeys(device.key),
            encryptionKeyPairProvider = EncryptionKeys(device.key),
            signatureCrypto = ReadableSignatures,
            transportCipher = ReadableTransportCipher,
            payloadCodec = DefaultTransportPayloadCodec(),
            routingIdGenerator = NameRoutingIds,
            localRoutingIdProvider = object : LocalRoutingIdProvider {
                override suspend fun getLocalRoutingId(): Result<String> = Result.success(device.routingId)
            },
            localBootstrapRoutingIdProvider = object : LocalBootstrapRoutingIdProvider {
                override suspend fun getLocalBootstrapRoutingId(): Result<String> = Result.success(device.alias)
            },
            json = json
        )

    private fun presenceClient(): HttpClient =
        HttpClient(
            MockEngine { request ->
                val lookup = json.decodeFromString<InvitationRouteLookup>((request.body as TextContent).text)
                val found = presence[lookup.routingId]
                val headers = headersOf(HttpHeaders.ContentType, "application/json")
                when {
                    presenceFails -> respond("", HttpStatusCode.ServiceUnavailable)
                    found == null -> respond("""{"code":"NO_INVITATION_ROUTE"}""", HttpStatusCode.NotFound, headers)
                    else -> respond(json.encodeToString(found), headers = headers)
                }
            }
        ) { expectSuccess = true }

    class Device(
        name: String
    ) {
        val key: ByteArray = name.encodeToByteArray()
        val routingId = "scrouting1_$name"
        val alias = "scphone1_$name"
        val route =
            MailboxDeliveryRoute(
                routeId = "route-$name",
                nodeId = "node-a",
                nodeEndpoint = "https://node-a.test",
                mailboxId = "invitations-$name",
                sendCapability = "send-$name",
                sequence = 0L,
                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                identitySignature = byteArrayOf()
            )
    }

    class RecordingInvitationGateway : MailboxGateway {
        val deposited = mutableListOf<FederatedEnvelope>()

        override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> {
            deposited += envelope
            return Result.success(Unit)
        }

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun createInvitation(
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> = error("Not used")

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ): Result<Unit> = error("Not used")

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun ready(
            accessEndpoint: String,
            credentials: List<LocalMailboxCredential>
        ): Result<Set<String>> = error("Not used")
    }

    private object Configuration : ControlPlaneConfiguration {
        override val endpoints = MutableStateFlow(listOf(ControlPlaneEndpoint("https://control.test")))
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
        override val manualBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>(null)

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = endpoints.value

        override fun markActive(endpoint: ControlPlaneEndpoint) = Unit

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = error("Not used")

        override suspend fun addManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun removeManual(baseUrl: String): Result<Unit> = error("Not used")

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = error("Not used")

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = error("Not used")
    }

    private object ZeroHash : CryptoHash {
        override fun sha256(input: ByteArray): ByteArray = ByteArray(32)
    }

    private class SigningKeys(
        private val key: ByteArray
    ) : LocalSigningKeyPairProvider {
        override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> = Result.success(LocalSigningKeyPair(key, key))
    }

    private class EncryptionKeys(
        private val key: ByteArray
    ) : LocalEncryptionKeyPairProvider {
        override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> = Result.success(LocalEncryptionKeyPair(key, key))
    }

    private object NameRoutingIds : RoutingIdGenerator {
        override fun deriveFromPhoneNumber(phoneNumber: String): Result<String> = error("Not used")

        override fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String> =
            Result.success("scrouting1_${signingPublicKey.decodeToString()}")
    }

    private object ReadableSignatures : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(signature(payload, signingPrivateKey))

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> =
            runCatching { check(signature.contentEquals(signature(payload, signingPublicKey))) { "Bad signature" } }

        private fun signature(
            payload: ByteArray,
            key: ByteArray
        ): ByteArray = key + payload.contentHashCode().toString().encodeToByteArray()
    }

    private object ReadableTransportCipher : TransportMessageCipher {
        override suspend fun encryptForRecipient(
            plaintext: ByteArray,
            recipientPublicKey: ByteArray
        ): Result<EncryptedTransportPayload> =
            Result.success(EncryptedTransportPayload(1, TransportEncryptionMode.SEALED_BOX, recipientPublicKey + byteArrayOf(0) + plaintext))

        override suspend fun decryptFromSender(
            encryptedPayload: EncryptedTransportPayload,
            localPublicKey: ByteArray,
            localPrivateKey: ByteArray
        ): Result<ByteArray> =
            runCatching {
                val prefix = localPublicKey + byteArrayOf(0)
                check(encryptedPayload.payload.copyOf(prefix.size).contentEquals(prefix)) { "Sealed for another key" }
                encryptedPayload.payload.copyOfRange(prefix.size, encryptedPayload.payload.size)
            }
    }
}
