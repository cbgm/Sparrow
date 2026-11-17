package com.cbgm.sparrow.server.security

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/** False for loopback, private, link-local, multicast and reserved addresses: outbound fetches must not reach them. */
fun InetAddress.isPublicUnicast(): Boolean =
    !isAnyLocalAddress &&
        !isLoopbackAddress &&
        !isLinkLocalAddress &&
        !isSiteLocalAddress &&
        !isMulticastAddress &&
        !isPrivateSpecialRange()

private fun InetAddress.isPrivateSpecialRange(): Boolean =
    when (this) {
        is Inet4Address -> {
            val value = address.fold(0) { result, byte -> (result shl 8) or (byte.toInt() and 0xff) }
            RESERVED_IPV4_RANGES.any { range -> range.contains(value) }
        }

        is Inet6Address -> {
            val bytes = address
            (bytes[0].toInt() and 0xfe) == 0xfc ||
                bytes.contentEquals(ByteArray(16)) ||
                isIpv4MappedPrivate(bytes)
        }

        else -> true
    }

/** An IPv4 network: [network] with its first [prefixLength] bits fixed. */
private class Ipv4Range(
    network: String,
    private val prefixLength: Int
) {
    private val network: Int = network.split('.').fold(0) { result, octet -> (result shl 8) or octet.toInt() }

    fun contains(address: Int): Boolean {
        val mask = if (prefixLength == 0) 0 else -1 shl (Int.SIZE_BITS - prefixLength)
        return address and mask == network and mask
    }
}

/** "This network", shared address space, IETF protocol assignments, documentation, benchmarking and reserved. */
private val RESERVED_IPV4_RANGES =
    listOf(
        Ipv4Range("0.0.0.0", 8),
        Ipv4Range("100.64.0.0", 10),
        Ipv4Range("192.0.0.0", 24),
        Ipv4Range("192.0.2.0", 24),
        Ipv4Range("198.18.0.0", 15),
        Ipv4Range("198.51.100.0", 24),
        Ipv4Range("203.0.113.0", 24),
        Ipv4Range("240.0.0.0", 4)
    )

private fun isIpv4MappedPrivate(bytes: ByteArray): Boolean {
    if (bytes.size != 16) return true
    val isMapped =
        bytes.take(10).all { it == 0.toByte() } &&
            bytes[10] == 0xff.toByte() &&
            bytes[11] == 0xff.toByte()
    if (!isMapped) return false

    val first = bytes[12].toInt() and 0xff
    val second = bytes[13].toInt() and 0xff
    return first == 10 ||
        first == 127 ||
        first == 169 && second == 254 ||
        first == 172 && second in 16..31 ||
        first == 192 && second == 168 ||
        first == 100 && second in 64..127
}
