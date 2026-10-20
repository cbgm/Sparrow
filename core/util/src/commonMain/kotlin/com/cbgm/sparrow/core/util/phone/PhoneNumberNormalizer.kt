package com.cbgm.sparrow.core.util.phone

interface PhoneNumberNormalizer {
    fun normalize(phoneNumber: String): Result<String>
}
