package com.cbgm.sparrow.feature.attachments.util

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

object LocationAttachmentPayload {
    fun encode(location: CurrentLocation): ByteArray =
        "${location.latitude},${location.longitude}".encodeToByteArray()

    fun decode(bytes: ByteArray): CurrentLocation? {
        val parts = bytes.decodeToString().split(',')
        if (parts.size != 2) return null

        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        return runCatching { CurrentLocation(latitude, longitude) }.getOrNull()
    }
}
