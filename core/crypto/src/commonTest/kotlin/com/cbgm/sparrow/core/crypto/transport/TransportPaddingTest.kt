package com.cbgm.sparrow.core.crypto.transport

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransportPaddingTest {
    @Test
    fun shortAndLongerPacketsLookTheSame() {
        assertEquals(1_024, TransportPadding.pad("ok".encodeToByteArray()).size)
        assertEquals(1_024, TransportPadding.pad(ByteArray(900) { 1 }).size)
        assertEquals(4_096, TransportPadding.pad(ByteArray(1_021) { 1 }).size)
        assertEquals(262_144, TransportPadding.pad(ByteArray(70_000) { 1 }).size)
        assertEquals(327_680, TransportPadding.pad(ByteArray(300_000) { 1 }).size)
    }

    @Test
    fun theReceiverGetsExactlyThePacketBack() {
        val packet = ByteArray(3_333) { (it % 251).toByte() }

        assertContentEquals(packet, TransportPadding.unpad(TransportPadding.pad(packet)))
    }

    @Test
    fun aBrokenLengthIsRejected() {
        val padded = TransportPadding.pad("hello".encodeToByteArray())
        padded[0] = 0x7F

        assertFailsWith<IllegalArgumentException> { TransportPadding.unpad(padded) }
    }
}
