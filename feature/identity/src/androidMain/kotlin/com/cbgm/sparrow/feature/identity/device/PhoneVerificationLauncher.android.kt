package com.cbgm.sparrow.feature.identity.device

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.core.logging.SparrowLog
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
actual fun PhoneVerificationLauncher(
    request: PhoneVerificationRequest?,
    onResult: (PhoneVerificationResult) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findPhoneVerificationActivity() }
    val currentOnResult = rememberUpdatedState(onResult)

    LaunchedEffect(request?.id) {
        when (request) {
            null -> Unit
            is PhoneVerificationRequest.Send -> {
                if (activity == null) {
                    currentOnResult.value(PhoneVerificationResult.Failed(request.id))
                    return@LaunchedEffect
                }
                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            verifyCredential(auth, credential, request.phoneNumber) { success ->
                                currentOnResult.value(
                                    if (success) {
                                        PhoneVerificationResult.Verified(request.id)
                                    } else {
                                        PhoneVerificationResult.Failed(request.id)
                                    }
                                )
                            }
                        }

                        override fun onVerificationFailed(exception: FirebaseException) {
                            SparrowLog.error("PhoneVerification", "Firebase verification failed", exception)
                            currentOnResult.value(PhoneVerificationResult.Failed(request.id))
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            currentOnResult.value(PhoneVerificationResult.CodeSent(request.id, verificationId))
                        }
                    }
                    PhoneAuthProvider.verifyPhoneNumber(
                        PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(request.phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                            .build()
                    )
                }.onFailure { error ->
                    SparrowLog.error("PhoneVerification", "Unable to start phone verification", error)
                    currentOnResult.value(PhoneVerificationResult.Failed(request.id))
                }
            }
            is PhoneVerificationRequest.Confirm -> {
                runCatching {
                    val credential = PhoneAuthProvider.getCredential(request.verificationId, request.code)
                    verifyCredential(FirebaseAuth.getInstance(), credential, request.phoneNumber) { success ->
                        currentOnResult.value(
                            if (success) {
                                PhoneVerificationResult.Verified(request.id)
                            } else {
                                PhoneVerificationResult.Failed(request.id)
                            }
                        )
                    }
                }.onFailure { error ->
                    SparrowLog.error("PhoneVerification", "Unable to confirm phone verification", error)
                    currentOnResult.value(PhoneVerificationResult.Failed(request.id))
                }
            }
        }
    }
}

private tailrec fun Context.findPhoneVerificationActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findPhoneVerificationActivity()
    else -> null
}

/** Complete only after Firebase has issued an ID token bound to the requested number. */
private fun verifyCredential(
    auth: FirebaseAuth,
    credential: PhoneAuthCredential,
    phoneNumber: String,
    onResult: (Boolean) -> Unit
) {
    auth.signInWithCredential(credential).addOnCompleteListener { signedIn ->
        val user = (if (signedIn.isSuccessful) signedIn.result?.user else null)
            ?.takeIf { it.phoneNumber == phoneNumber }
        if (user == null) {
            onResult(false)
            return@addOnCompleteListener
        }
        user.getIdToken(false).addOnCompleteListener { tokenResult ->
            onResult(tokenResult.isSuccessful && !tokenResult.result?.token.isNullOrBlank())
        }
    }
}
