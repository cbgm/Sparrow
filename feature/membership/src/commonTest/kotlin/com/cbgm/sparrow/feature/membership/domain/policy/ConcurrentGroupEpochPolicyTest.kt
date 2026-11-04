package com.cbgm.sparrow.feature.membership.domain.policy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConcurrentGroupEpochPolicyTest {
    @Test
    fun theLowerSigningKeyWinsComparedAsUnsignedBytes() {
        assertTrue(ConcurrentGroupEpochPolicy.replacesInstalled(byteArrayOf(0x01, 0x7F), byteArrayOf(0x01, 0x80.toByte())))
        assertFalse(ConcurrentGroupEpochPolicy.replacesInstalled(byteArrayOf(0x80.toByte()), byteArrayOf(0x7F)))
    }

    @Test
    fun everyDevicePicksTheSameVersion() {
        val first = byteArrayOf(3, 9)
        val second = byteArrayOf(3, 10)
        assertTrue(ConcurrentGroupEpochPolicy.replacesInstalled(first, second))
        assertFalse(ConcurrentGroupEpochPolicy.replacesInstalled(second, first))
        assertFalse(ConcurrentGroupEpochPolicy.replacesInstalled(first, first))
    }
}
