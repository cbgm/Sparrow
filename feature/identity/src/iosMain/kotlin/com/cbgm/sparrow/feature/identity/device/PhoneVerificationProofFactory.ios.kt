package com.cbgm.sparrow.feature.identity.device

import com.cbgm.sparrow.core.phone.PhoneVerificationProofProvider

actual fun createPhoneVerificationProofProvider(): PhoneVerificationProofProvider =
    object : PhoneVerificationProofProvider {
        override suspend fun tokenFor(phoneNumber: String): String? = null
    }
