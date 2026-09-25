package com.cbgm.sparrow.server.linkpreview

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException

internal object LinkPreviewUrlValidator {
    fun validate(rawUrl: String): URI {
        val uri =
            try {
                URI(rawUrl)
            } catch (exception: URISyntaxException) {
                throw IllegalArgumentException("Invalid link-preview URL", exception)
            }
        require(uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) {
            "Link preview only supports HTTP and HTTPS"
        }
        require(uri.userInfo == null) {
            "Link-preview URLs must not contain user info"
        }
        val host = uri.host?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Link-preview URL has no host")

        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty()) {
            "Link-preview host could not be resolved"
        }
        addresses.forEach(::requirePublicAddress)
        return uri
    }

    private fun requirePublicAddress(address: InetAddress) {
        require(
            !address.isAnyLocalAddress &&
                !address.isLoopbackAddress &&
                !address.isLinkLocalAddress &&
                !address.isSiteLocalAddress &&
                !address.isMulticastAddress &&
                !address.isPrivateSpecialRange()
        ) {
            "Link-preview URL resolves to a non-public address"
        }
    }
}

private fun InetAddress.isPrivateSpecialRange(): Boolean =
    when (this) {
        is Inet4Address -> {
            val octets = address.map(Byte::toInt).map { it and 0xff }
            val first = octets[0]
            val second = octets[1]
            when {
                first == 0 -> true
                first == 100 && second in 64..127 -> true
                first == 192 && second == 0 && octets[2] == 0 -> true
                first == 192 && second == 0 && octets[2] == 2 -> true
                first == 198 && second in 18..19 -> true
                first == 198 && second == 51 && octets[2] == 100 -> true
                first == 203 && second == 0 && octets[2] == 113 -> true
                first >= 240 -> true
                else -> false
            }
        }

        is Inet6Address -> {
            val bytes = address
            (bytes[0].toInt() and 0xfe) == 0xfc ||
                bytes.contentEquals(ByteArray(16)) ||
                isIpv4MappedPrivate(bytes)
        }

        else -> true
    }

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
