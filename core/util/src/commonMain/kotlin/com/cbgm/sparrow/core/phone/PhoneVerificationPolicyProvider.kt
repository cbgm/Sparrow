package com.cbgm.sparrow.core.phone

/** Server policy: a failed lookup must never be treated as optional. */
interface PhoneVerificationPolicyProvider {
    suspend fun isRequired(): Result<Boolean>
}
