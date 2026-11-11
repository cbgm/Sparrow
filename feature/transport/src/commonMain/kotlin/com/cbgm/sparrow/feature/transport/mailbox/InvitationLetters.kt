package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.presence.InvitationRouteRemoteSource
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Packets for a device whose mailbox route the sender does not have yet: a first invitation to a phone alias, or the
 * answers to a contact before its route arrived. The sender looks up the device's invitation mailbox at its Control
 * Planes ([InvitationRouteRemoteSource]) and puts a letter there through its circuit: the transport payload with the
 * sender's routing ID, signed with the sender's device key and sealed for the recipient's key, so the mailbox's node
 * reads neither the packet nor who sent it.
 *
 * The recipient trusts the sender's routing ID only when the signing key proves it: a device routing ID derives from
 * the key, and a phone alias must belong to that device at the recipient's Control Planes. Nobody can claim another
 * person's phone number this way.
 */
interface InvitationLetters {
    /** Puts [payload] into the invitation mailbox of [recipientRoutingId]; returns when the envelope expires there. */
    suspend fun send(
        senderRoutingId: String,
        recipientRoutingId: String,
        payload: String,
        envelopeId: String,
        createdAtEpochMilliseconds: Long
    ): Result<Long>

    /**
     * The sender and transport payload of an envelope from this device's invitation mailbox. Fails with
     * [InvalidInvitationLetterException] when the letter can never be accepted (not for this device, a bad signature,
     * a routing ID the key does not prove), and with other failures when it may succeed later.
     */
    suspend fun open(envelope: FederatedEnvelope): Result<InvitationLetterContent>
}

/** [InvitationLetters] signed with the device key and sealed with the transport cipher. */
internal class SignedInvitationLetters(
    private val routes: InvitationRouteRemoteSource,
    private val mailboxGateway: MailboxGateway,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val encryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val transportCipher: TransportMessageCipher,
    private val payloadCodec: TransportPayloadCodec,
    private val routingIdGenerator: RoutingIdGenerator,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val json: Json
) : InvitationLetters {
    override suspend fun send(
        senderRoutingId: String,
        recipientRoutingId: String,
        payload: String,
        envelopeId: String,
        createdAtEpochMilliseconds: Long
    ): Result<Long> =
        safeSuspendCall(logFailure = false) {
            val invitation = (routes.find(recipientRoutingId).getOrThrow() ?: throw InvitationRouteMissingException()).invitationRoute
            val keyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val unsigned = UnsignedInvitationLetter(senderRoutingId, recipientRoutingId, keyPair.publicKey, payload, createdAtEpochMilliseconds)
            val signature = signatureCrypto.sign(unsigned.signedBytes(), keyPair.privateKey).getOrThrow()
            val letter = json.encodeToString(unsigned.signed(signature)).encodeToByteArray()
            val sealed = transportCipher.encryptForRecipient(letter, invitation.encryptionPublicKey).getOrThrow()
            val expiresAt = minOf(createdAtEpochMilliseconds + ENVELOPE_LIFETIME_MILLISECONDS, invitation.route.expiresAtEpochMilliseconds)
            mailboxGateway
                .depositInvitation(
                    FederatedEnvelope(
                        envelopeId = envelopeId,
                        senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        mailboxRoute = invitation.route,
                        encryptedPayload = payloadCodec.encode(sealed),
                        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = expiresAt
                    )
                ).getOrThrow()
            expiresAt
        }

    override suspend fun open(envelope: FederatedEnvelope): Result<InvitationLetterContent> =
        safeSuspendCall(logFailure = false) {
            val letter = read(envelope.encryptedPayload)
            signatureCrypto
                .verify(letter.unsigned().signedBytes(), letter.signingPublicKey, letter.signature)
                .getOrElse { throw InvalidInvitationLetterException("Invitation letter signature does not verify") }
            val ownRoutingIds =
                listOfNotNull(
                    localRoutingIdProvider.getLocalRoutingId().getOrThrow(),
                    localBootstrapRoutingIdProvider.getLocalBootstrapRoutingId().getOrNull()
                )
            if (letter.recipientRoutingId !in ownRoutingIds) throw InvalidInvitationLetterException("Invitation letter is for another device")
            val device = routingIdGenerator.deriveFromSigningPublicKey(letter.signingPublicKey).getOrThrow()
            if (!proves(letter.senderRoutingId, device)) throw InvalidInvitationLetterException("The sender's key does not prove its routing ID")
            InvitationLetterContent(senderRoutingId = letter.senderRoutingId, payload = letter.payload)
        }

    private suspend fun read(encryptedPayload: String): InvitationLetter {
        val keys = encryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
        val sealed =
            payloadCodec
                .decode(encryptedPayload)
                .getOrNull()
                ?.takeIf { payload -> payload.mode == TransportEncryptionMode.SEALED_BOX }
                ?: throw InvalidInvitationLetterException("Invitation letter is not sealed")
        val plaintext =
            transportCipher
                .decryptFromSender(sealed, keys.publicKey, keys.privateKey)
                .getOrElse { throw InvalidInvitationLetterException("Invitation letter does not open") }
        return runCatching { json.decodeFromString<InvitationLetter>(plaintext.decodeToString()) }
            .getOrElse { throw InvalidInvitationLetterException("Invitation letter is unreadable") }
    }

    /** A device routing ID derives from the key; a phone alias must belong to that [device] at the Control Planes. */
    private suspend fun proves(
        senderRoutingId: String,
        device: String
    ): Boolean =
        when {
            senderRoutingId == device -> true
            senderRoutingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) -> routes.find(senderRoutingId).getOrThrow()?.ownerRoutingId == device
            else -> false
        }

    private fun UnsignedInvitationLetter.signedBytes(): ByteArray = (SIGNATURE_DOMAIN + json.encodeToString(this)).encodeToByteArray()

    private companion object {
        const val SIGNATURE_DOMAIN = "sparrow-invitation-letter-v1\n"
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}

/** What a letter from this device's invitation mailbox carries, after its sender was proven. */
data class InvitationLetterContent(
    val senderRoutingId: String,
    val payload: String
)

/** The letter can never be accepted; its envelope is acknowledged and dropped. */
class InvalidInvitationLetterException(
    message: String
) : IllegalArgumentException(message)

/** None of the user's Control Planes knows an invitation mailbox of the recipient. */
class InvitationRouteMissingException : IllegalStateException("The recipient has no invitation mailbox at any Control Plane")

@Serializable
internal class UnsignedInvitationLetter(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val signingPublicKey: ByteArray,
    val payload: String,
    val createdAtEpochMilliseconds: Long
) {
    fun signed(signature: ByteArray): InvitationLetter =
        InvitationLetter(senderRoutingId, recipientRoutingId, signingPublicKey, payload, createdAtEpochMilliseconds, signature)
}

@Serializable
internal class InvitationLetter(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val signingPublicKey: ByteArray,
    val payload: String,
    val createdAtEpochMilliseconds: Long,
    val signature: ByteArray
) {
    fun unsigned(): UnsignedInvitationLetter =
        UnsignedInvitationLetter(senderRoutingId, recipientRoutingId, signingPublicKey, payload, createdAtEpochMilliseconds)

    override fun toString(): String = "InvitationLetter(redacted)"
}
