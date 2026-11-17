package com.cbgm.sparrow.feature.transport.network

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalNetworkAddressesTest {
    @Test
    fun localAddressesAreRecognizedAndPublicOnesAreNot() {
        val expected =
            mapOf(
                "http://192.168.178.60:8390" to true,
                "ws://10.0.2.2:8494/v1/gateway" to true,
                "https://172.16.0.1" to true,
                "https://172.31.255.255/v1/control-planes" to true,
                "http://169.254.10.1" to true,
                "https://sparrow.local" to true,
                "wss://node-192-168-178-60.sslip.io/v1/gateway" to true,
                "https://10.0.0.5.nip.io" to true,
                "http://[fd12:3456::1]:8390" to true,
                "http://[fe80::1]" to true,
                "https://172.32.0.1" to false,
                "https://8.8.8.8" to false,
                "wss://node-80-131-151-133.sslip.io/v1/gateway" to false,
                "https://directory.example.com" to false,
                "https://192.168.example.com" to false,
                "http://[2001:db8::1]" to false
            )

        expected.forEach { (url, local) -> assertEquals(local, LocalNetworkAddresses.isLocal(url), url) }
    }
}
