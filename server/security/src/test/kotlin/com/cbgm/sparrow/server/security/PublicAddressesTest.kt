package com.cbgm.sparrow.server.security

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicAddressesTest {
    @Test
    fun reservedIpv4RangesAreNotPublicAndTheirNeighboursAre() {
        val expected =
            mapOf(
                "8.8.8.8" to true,
                "0.1.2.3" to false,
                "10.0.0.1" to false,
                "127.0.0.1" to false,
                "100.63.255.255" to true,
                "100.64.0.1" to false,
                "100.127.255.255" to false,
                "100.128.0.1" to true,
                "192.0.0.5" to false,
                "192.0.1.1" to true,
                "192.0.2.1" to false,
                "198.17.255.255" to true,
                "198.18.0.1" to false,
                "198.19.255.255" to false,
                "198.20.0.0" to true,
                "198.51.100.7" to false,
                "203.0.113.9" to false,
                "203.0.114.1" to true,
                "239.255.255.255" to false,
                "240.0.0.1" to false,
                "255.255.255.255" to false
            )

        expected.forEach { (address, public) ->
            assertEquals(public, InetAddress.getByName(address).isPublicUnicast(), address)
        }
    }
}
