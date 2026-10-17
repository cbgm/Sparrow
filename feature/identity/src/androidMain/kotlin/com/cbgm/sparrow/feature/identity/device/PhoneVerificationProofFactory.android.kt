package com.cbgm.sparrow.feature.identity.device

import com.cbgm.sparrow.core.phone.PhoneVerificationProofProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createPhoneVerificationProofProvider(): PhoneVerificationProofProvider =
    object : PhoneVerificationProofProvider {
        override suspend fun tokenFor(phoneNumber: String): String? {
            val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
                ?: return null
            // Never attach a proof from a different phone/account to a signed routing alias.
            if (user.phoneNumber != phoneNumber) return null
            return suspendCancellableCoroutine { continuation ->
                user.getIdToken(false).addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(if (task.isSuccessful) task.result?.token else null)
                    }
                }
            }
        }
    }
