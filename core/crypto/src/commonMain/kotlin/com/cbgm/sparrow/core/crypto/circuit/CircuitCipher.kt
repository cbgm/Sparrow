package com.cbgm.sparrow.core.crypto.circuit

/** The two keys of one circuit hop: [forward] for cells towards the session node, [backward] for cells back. */
class CircuitHopKeys(
    val forward: ByteArray,
    val backward: ByteArray
) {
    override fun toString(): String = "CircuitHopKeys(redacted)"
}

/** A one-time X25519 key for one hop handshake; [publicKey] is sent to the relay. */
class CircuitEphemeralKey(
    internal val secretKey: ByteArray,
    val publicKey: ByteArray
) {
    override fun toString(): String = "CircuitEphemeralKey(redacted)"
}

/**
 * The app's side of the circuit crypto, matching the server's `CircuitHandshake` and `CircuitLayer`: a one round
 * trip handshake per hop that proves the relay holds its published relay key, ChaCha20 layers for the hops in between
 * and ChaCha20-Poly1305 for the circuit's last hop. Each direction of each hop counts its cells; the count is the
 * nonce.
 */
interface CircuitCipher {
    suspend fun newEphemeralKey(): CircuitEphemeralKey

    /** The hop's keys, or null when the relay did not prove its relay key. */
    suspend fun completeHandshake(
        ephemeralKey: CircuitEphemeralKey,
        relayPublicKey: ByteArray,
        reply: ByteArray
    ): CircuitHopKeys?

    /** Adds or removes one ChaCha20 layer; the size stays. */
    suspend fun stream(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray

    suspend fun seal(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray

    /** Null when [sealed] was not sealed with [key] and [counter], or changed on the way. */
    suspend fun open(
        key: ByteArray,
        counter: Long,
        sealed: ByteArray
    ): ByteArray?
}
