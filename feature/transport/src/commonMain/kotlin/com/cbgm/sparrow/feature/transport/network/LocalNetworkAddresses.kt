package com.cbgm.sparrow.feature.transport.network

/**
 * Whether an address is on the local network, which Android 17 lets an app reach only with the local network
 * permission: private and link-local IPv4 and IPv6 literals, `.local` names, and sslip.io/nip.io names that carry such an
 * IPv4 (for example `192-168-178-60.sslip.io`). Other names are taken as public; what they resolve to is not checked.
 */
object LocalNetworkAddresses {
    fun isLocal(url: String): Boolean {
        val host = hostOf(url).lowercase()
        return host.endsWith(".local") || isLocalIpv6(host) || ipv4In(host)?.let(::isLocalIpv4) == true
    }

    private fun hostOf(url: String): String {
        val authority = url.substringAfter("://").substringBefore('/').substringBefore('?').substringAfter('@')
        return if (authority.startsWith("[")) authority.substringAfter('[').substringBefore(']') else authority.substringBefore(':')
    }

    /** The host itself as an IPv4, or one written into an sslip.io/nip.io name with dots or dashes. */
    private fun ipv4In(host: String): List<Int>? {
        parseIpv4(host)?.let { return it }
        val label = WILDCARD_DNS_SUFFIXES.firstOrNull(host::endsWith)?.let { suffix -> host.removeSuffix(suffix) } ?: return null
        return IPV4_IN_NAME.find(label)?.value?.replace('-', '.')?.let(::parseIpv4)
    }

    private fun parseIpv4(value: String): List<Int>? =
        value
            .split('.')
            .takeIf { parts -> parts.size == IPV4_PARTS }
            ?.map { part -> part.toIntOrNull()?.takeIf { it in 0..MAXIMUM_OCTET } ?: return null }

    private fun isLocalIpv4(octets: List<Int>): Boolean {
        val (first, second) = octets
        return first == 10 ||
            first == 172 && second in 16..31 ||
            first == 192 && second == 168 ||
            first == 169 && second == 254
    }

    /** Unique local (fc00::/7) and link-local (fe80::/10) addresses. */
    private fun isLocalIpv6(host: String): Boolean = ':' in host && LOCAL_IPV6_PREFIXES.any(host::startsWith)

    private const val IPV4_PARTS = 4
    private const val MAXIMUM_OCTET = 255
    private val LOCAL_IPV6_PREFIXES = listOf("fc", "fd", "fe8", "fe9", "fea", "feb")
    private val WILDCARD_DNS_SUFFIXES = listOf(".sslip.io", ".nip.io")
    private val IPV4_IN_NAME = Regex("""\d{1,3}[.-]\d{1,3}[.-]\d{1,3}[.-]\d{1,3}$""")
}
