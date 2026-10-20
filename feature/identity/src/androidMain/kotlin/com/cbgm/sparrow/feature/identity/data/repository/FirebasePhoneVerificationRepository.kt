package com.cbgm.sparrow.feature.identity.data.repository

import android.app.Activity
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.device.DebugSmsNotifier
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase SMS verification. The app signs in only to obtain the one-time token for the Control
 * Plane and signs out right away, so Firebase is not contacted afterwards. With [testSmsCode]
 * (debug builds with DEBUG_SMS_CODE) Firebase test mode is used: no app check for its test
 * numbers, and the test code arrives as if the SMS had been read.
 */
class FirebasePhoneVerificationRepository(
    private val testSmsCode: String?,
    private val debugSms: DebugSmsNotifier
) : PhoneVerificationRepository {
    private var activity = WeakReference<Activity>(null)
    private var resend: Pair<String, PhoneAuthProvider.ForceResendingToken>? = null
    private val automaticCredentials = mutableMapOf<String, PhoneAuthCredential>()

    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (this.activity.get() === activity) this.activity = WeakReference(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun sendCode(
        phoneNumber: String,
        onVerifiedAutomatically: (String?) -> Unit
    ): Result<String> = safeSuspendCall(logFailure = false) {
        val host = checkNotNull(activity.get()) { "Phone verification needs a visible screen" }
        val auth = FirebaseAuth.getInstance()
        if (testSmsCode != null) {
            debugSms.show(testSmsCode)
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            auth.firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber(
                phoneNumber,
                testSmsCode
            )
        }
        val verificationId = CompletableDeferred<String>()
        val callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    resend = phoneNumber to token
                    verificationId.complete(id)
                }

                // Instant verification, or the SMS was read automatically.
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val id =
                        if (verificationId.isCompleted) verificationId.getCompleted() else INSTANT
                    automaticCredentials[id] = credential
                    verificationId.complete(id)
                    onVerifiedAutomatically(credential.smsCode)
                }

                override fun onVerificationFailed(error: FirebaseException) {
                    verificationId.completeExceptionally(error)
                }
            }
        val options =
            PhoneAuthOptions
                .newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(host)
                .setCallbacks(callbacks)
        resend?.takeIf { it.first == phoneNumber }?.let { options.setForceResendingToken(it.second) }
        automaticCredentials.clear()
        PhoneAuthProvider.verifyPhoneNumber(options.build())
        verificationId.await()
    }

    override suspend fun confirmCode(
        verificationId: String,
        code: String
    ): Result<String> = safeSuspendCall(logFailure = false) {
        val credential =
            automaticCredentials.remove(verificationId)
                ?: PhoneAuthProvider.getCredential(verificationId, code)
        val auth = FirebaseAuth.getInstance()
        try {
            val user = checkNotNull(auth.signInWithCredential(credential).await().user)
            resend = null
            checkNotNull(user.getIdToken(true).await().token)
        } finally {
            auth.signOut()
        }
    }

    private companion object {
        const val CODE_TIMEOUT_SECONDS = 60L
        const val INSTANT = "instant"
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            val error = task.exception
            when {
                error != null -> continuation.resumeWithException(error)
                task.isCanceled -> continuation.cancel()
                else -> continuation.resume(task.result)
            }
        }
    }
