package com.cbgm.sparrow.core.protocol.phone

class DefaultPhoneNumberNormalizer : PhoneNumberNormalizer {
    override fun normalize(phoneNumber: String): Result<String> =
        runCatching {
            val trimmed = phoneNumber.trim()

            require(trimmed.isNotEmpty()) {
                "Phone number must not be blank"
            }

            val digits = trimmed.filter { character -> character.isDigit() }

            require(digits.isNotEmpty()) {
                "Phone number contains no digits"
            }

            val normalized =
                when {
                    trimmed.startsWith("+") -> {
                        "+$digits"
                    }

                    digits.startsWith("00") -> {
                        "+${digits.drop(2)}"
                    }

                    else -> {
                        digits
                    }
                }

            require(normalized.count { character -> character.isDigit() } >= MINIMUM_DIGIT_COUNT) {
                "Phone number is too short"
            }

            normalized
        }

    private companion object {
        const val MINIMUM_DIGIT_COUNT = 5
    }
}
