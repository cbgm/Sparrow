package com.cbgm.sparrow.feature.identity.data.repository

import android.app.Activity
import android.content.Context
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.device.DebugSmsNotifier
import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationStep
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase SMS verification. The app signs in only to obtain the one-time token for the Control
 * Plane and signs out right away, so Firebase is not contacted afterwards. With [testSmsCode]
 * (debug builds with DEBUG_SMS_CODE) Firebase test mode is used: no app check for its test
 * numbers, and the test code arrives as if the SMS had been read. The app's Firebase project comes from
 * [firebaseProject] (local.properties), not from a google-services.json; without it phone verification fails
 * with a message instead of starting Firebase.
 */
class FirebasePhoneVerificationRepository(
    private val context: Context,
    private val firebaseProject: FirebaseProject?,
    private val testSmsCode: String?,
    private val debugSms: DebugSmsNotifier
) : PhoneVerificationRepository {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance(firebaseApp())
    private var activity = WeakReference<Activity>(null)
    private var resend: Pair<String, PhoneAuthProvider.ForceResendingToken>? = null

    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (this.activity.get() === activity) this.activity = WeakReference(null)
    }

    override fun sendCode(phoneNumber: String): Flow<Result<PhoneVerificationStep>> =
        callbackFlow {
            val callbacks =
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(
                        id: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        resend = phoneNumber to token
                        trySend(PhoneVerificationStep.CodeSent(id))
                    }

                    // Instant verification, or the SMS was read automatically.
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        trySend(PhoneVerificationStep.CodeReceived(credential.smsCode))
                        launch {
                            send(PhoneVerificationStep.Verified(token = signIn(credential)))
                            close()
                        }
                    }

                    override fun onVerificationFailed(error: FirebaseException) {
                        close(error)
                    }
                }
            val options = options(phoneNumber, callbacks)
            testSmsCode?.let { useTestMode(phoneNumber, it) }
            PhoneAuthProvider.verifyPhoneNumber(options)
            awaitClose()
        }.map { step -> Result.success(step) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(Result.failure(error))
            }

    override suspend fun confirmCode(
        verificationId: String,
        code: String
    ): Result<String> = safeSuspendCall(logFailure = false) {
        signIn(PhoneAuthProvider.getCredential(verificationId, code))
    }

    private fun options(
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): PhoneAuthOptions {
        val host = checkNotNull(activity.get()) { "Phone verification needs a visible screen" }
        val options =
            PhoneAuthOptions
                .newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(CODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(host)
                .setCallbacks(callbacks)
        resend
            ?.takeIf { it.first == phoneNumber }
            ?.let { options.setForceResendingToken(it.second) }
        return options.build()
    }

    private fun useTestMode(
        phoneNumber: String,
        code: String
    ) {
        debugSms.show(code)
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        auth.firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, code)
    }

    // Signs in only for the ID token and signs out right away.
    private suspend fun signIn(credential: PhoneAuthCredential): String =
        try {
            val user = checkNotNull(auth.signInWithCredential(credential).await().user)
            resend = null
            checkNotNull(user.getIdToken(true).await().token)
        } finally {
            auth.signOut()
        }

    private fun firebaseApp(): FirebaseApp {
        val project = checkNotNull(firebaseProject) { "Phone verification is not set up in this build" }
        return synchronized(FirebaseApp::class.java) {
            FirebaseApp.getApps(context).firstOrNull { app -> app.name == FIREBASE_APP_NAME }
                ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions
                        .Builder()
                        .setApplicationId(project.appId)
                        .setApiKey(project.apiKey)
                        .setProjectId(project.projectId)
                        .build(),
                    FIREBASE_APP_NAME
                )
        }
    }

    private companion object {
        const val FIREBASE_APP_NAME = "sparrow-phone-verification"
        const val CODE_TIMEOUT_SECONDS = 60L
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

/** The app's Firebase project (FIREBASE_APP_ID, FIREBASE_API_KEY and FIREBASE_PROJECT_ID in local.properties). */
class FirebaseProject(
    val appId: String,
    val apiKey: String,
    val projectId: String
) {
    companion object {
        /** Null unless all three values are set. */
        fun of(
            appId: String?,
            apiKey: String?,
            projectId: String?
        ): FirebaseProject? =
            if (appId.isNullOrBlank() || apiKey.isNullOrBlank() || projectId.isNullOrBlank()) {
                null
            } else {
                FirebaseProject(appId, apiKey, projectId)
            }
    }
}
