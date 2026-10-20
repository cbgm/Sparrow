package com.cbgm.sparrow.server.presence

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

internal class FirebasePhoneTokenVerifier(
    private val auth: () -> FirebaseAuth
) : PhoneTokenVerifier {
    override suspend fun verify(token: String): VerifiedPhone =
        withContext(Dispatchers.IO) {
            val decoded = auth().verifyIdToken(token, true)
            val firebase = decoded.claims["firebase"] as? Map<*, *>
            require(firebase?.get("sign_in_provider") == "phone") { "Phone sign-in required" }
            VerifiedPhone(
                phoneNumber = requireNotNull(decoded.claims["phone_number"] as? String),
                expiresAtEpochMilliseconds =
                    requireNotNull(decoded.claims["exp"] as? Number).toLong() *
                        MILLISECONDS_PER_SECOND
            )
        }

    private companion object {
        const val MILLISECONDS_PER_SECOND = 1_000L
    }
}

internal fun createPhoneVerification(
    environment: (String) -> String? = System::getenv
): PhoneVerification {
    val configured =
        environment("PHONE_VERIFICATION_REQUIRED")?.takeIf(String::isNotBlank) ?: "false"
    val required =
        requireNotNull(configured.toBooleanStrictOrNull()) {
            "PHONE_VERIFICATION_REQUIRED must be true or false"
        }
    val credentials = environment("GOOGLE_APPLICATION_CREDENTIALS")?.takeIf(String::isNotBlank)
    val projectId =
        environment("FIREBASE_TARGET_PROJECT_ID")?.takeIf(String::isNotBlank)
            ?: SPARROW_FIREBASE_PROJECT_ID
    if (credentials == null && required) {
        LoggerFactory
            .getLogger(PhoneVerification::class.java)
            .error("Phone verification is required but no Firebase credentials are mounted")
    }
    val verifier =
        credentials?.let {
            val auth by lazy { firebaseAuth(projectId) }
            CachingPhoneTokenVerifier(FirebasePhoneTokenVerifier { auth })
        }
    return PhoneVerification(required = required, verifier = verifier)
}

private fun firebaseAuth(projectId: String): FirebaseAuth {
    val app =
        synchronized(FirebaseApp::class.java) {
            FirebaseApp.getApps().firstOrNull { it.name == FIREBASE_APP_NAME }
                ?: FirebaseApp.initializeApp(
                    FirebaseOptions
                        .builder()
                        .setProjectId(projectId)
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build(),
                    FIREBASE_APP_NAME
                )
        }
    return FirebaseAuth.getInstance(app)
}

private const val FIREBASE_APP_NAME = "sparrow-phone-verification"
private const val SPARROW_FIREBASE_PROJECT_ID = "sparrow-a9048"
