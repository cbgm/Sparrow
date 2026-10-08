package com.cbgm.sparrow.core.phone

interface PhoneNumberNormalizer {
    fun normalize(phoneNumber: String): Result<String>
}
