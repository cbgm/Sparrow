package com.cbgm.sparrow.core.protocol.phone

interface PhoneNumberNormalizer {
    fun normalize(phoneNumber: String): Result<String>
}
