package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.data.controlplane.InvitationRouteRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.model.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.domain.model.InvitationLetterContent
import com.cbgm.sparrow.feature.transport.domain.model.InvitationRouteMissingException
import com.cbgm.sparrow.feature.transport.domain.repository.InvitationLetterRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** [InvitationLetterRepository] signed with the device key and sealed with the transport cipher. */
internal class InvitationLetterRepositoryImpl(
    private val routes: InvitationRouteRemoteDataSource,
    private val mailboxDataSource: MailboxRemoteDataSource,
    private val signingKeyPairSource: LocalSigningKeyPairSource,
    private val encryptionKeyPairSource: LocalEncryptionKeyPairSource,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val transportCipher: TransportMessageCipher,
    private val payloadCodec: TransportPayloadCodec,
    private val routingIdGenerator: RoutingIdGenerator,
    private val routingIdDataSource: RoutingIdLocalDataSource,
    private val json: Json
) : InvitationLetterRepository {
    override suspend fun send(
        senderRoutingId: String,
        recipientRoutingId: String,
        payload: String,
        envelopeId: String,
        createdAtEpochMilliseconds: Long
    ): Result<Long> =
        safeSuspendCall(logFailure = false) {
            val invitation = (routes.find(recipientRoutingId).getOrThrow() ?: throw InvitationRouteMissingException()).invitationRoute
            val keyPair = signingKeyPairSource.getSigningKeyPair().getOrThrow()
            val unsigned = UnsignedInvitationLetterDto(senderRoutingId, recipientRoutingId, keyPair.publicKey, payload, createdAtEpochMilliseconds)
            val signature = signatureCrypto.sign(unsigned.signedBytes(), keyPair.privateKey).getOrThrow()
            val letter = json.encodeToString(unsigned.signed(signature)).encodeToByteArray()
            val sealed = transportCipher.encryptForRecipient(letter, invitation.encryptionPublicKey).getOrThrow()
            val expiresAt = minOf(createdAtEpochMilliseconds + ENVELOPE_LIFETIME_MILLISECONDS, invitation.route.expiresAtEpochMilliseconds)
            mailboxDataSource
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
                    routingIdDataSource.getLocalRoutingId().getOrThrow(),
                    routingIdDataSource.getLocalBootstrapRoutingId().getOrNull()
                )
            if (letter.recipientRoutingId !in ownRoutingIds) throw InvalidInvitationLetterException("Invitation letter is for another device")
            val device = routingIdGenerator.deriveFromSigningPublicKey(letter.signingPublicKey).getOrThrow()
            if (!proves(letter.senderRoutingId, device)) throw InvalidInvitationLetterException("The sender's key does not prove its routing ID")
            InvitationLetterContent(senderRoutingId = letter.senderRoutingId, payload = letter.payload)
        }

    private suspend fun read(encryptedPayload: String): InvitationLetterDto {
        val keys = encryptionKeyPairSource.getEncryptionKeyPair().getOrThrow()
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
        return runCatching { json.decodeFromString<InvitationLetterDto>(plaintext.decodeToString()) }
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

    private fun UnsignedInvitationLetterDto.signedBytes(): ByteArray = (SIGNATURE_DOMAIN + json.encodeToString(this)).encodeToByteArray()

    private companion object {
        const val SIGNATURE_DOMAIN = "sparrow-invitation-letter-v1\n"
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val ENVELOPE_LIFETIME_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}

@Serializable
internal class UnsignedInvitationLetterDto(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val signingPublicKey: ByteArray,
    val payload: String,
    val createdAtEpochMilliseconds: Long
) {
    fun signed(signature: ByteArray): InvitationLetterDto =
        InvitationLetterDto(senderRoutingId, recipientRoutingId, signingPublicKey, payload, createdAtEpochMilliseconds, signature)
}

@Serializable
internal class InvitationLetterDto(
    val senderRoutingId: String,
    val recipientRoutingId: String,
    val signingPublicKey: ByteArray,
    val payload: String,
    val createdAtEpochMilliseconds: Long,
    val signature: ByteArray
) {
    fun unsigned(): UnsignedInvitationLetterDto =
        UnsignedInvitationLetterDto(senderRoutingId, recipientRoutingId, signingPublicKey, payload, createdAtEpochMilliseconds)

    override fun toString(): String = "InvitationLetterDto(redacted)"
}
