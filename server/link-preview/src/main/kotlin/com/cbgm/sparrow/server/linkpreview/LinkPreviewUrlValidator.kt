package com.cbgm.sparrow.server.linkpreview

import com.cbgm.sparrow.server.security.isPublicUnicast
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
        require(address.isPublicUnicast()) {
            "Link-preview URL resolves to a non-public address"
        }
    }
}
