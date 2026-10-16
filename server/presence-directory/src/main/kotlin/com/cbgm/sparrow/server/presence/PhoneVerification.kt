package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneVerificationPolicy(
    val required: Boolean
)

/** Invalid supplied proofs fail even in optional mode. Tokens are never stored with presence routes. */
internal class PhoneVerification(
    val required: Boolean,
    private val verifiedPhone: suspend (String) -> String
) {
    suspend fun accepts(registration: ClientRouteRegistration): Boolean {
        val aliases = registration.route.aliases.orEmpty()
        if (aliases.isEmpty()) return true
        val token = registration.phoneVerificationToken
        if (token == null) return !required
        if (token.isBlank()) return false
        val phone = try {
            verifiedPhone(token)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return false
        }
        return runCatching {
            val expectedAlias = ClientRoutingIds.fromPhoneNumber(phone)
            aliases.all { it == expectedAlias }
        }.getOrDefault(false)
    }

    companion object {
        fun fromEnvironment(): PhoneVerification {
            val configured = System.getenv("PHONE_VERIFICATION_REQUIRED") ?: "false"
            val required = requireNotNull(configured.toBooleanStrictOrNull()) {
                "PHONE_VERIFICATION_REQUIRED must be true or false"
            }
            val projectId = System.getenv("FIREBASE_TARGET_PROJECT_ID")?.takeIf(String::isNotBlank)
            val auth by lazy {
                require(!projectId.isNullOrBlank()) { "FIREBASE_TARGET_PROJECT_ID is required" }
                val app = synchronized(FirebaseApp::class.java) {
                    FirebaseApp.getApps().firstOrNull { it.name == "sparrow-phone-verification" }
                        ?: FirebaseApp.initializeApp(
                            FirebaseOptions.builder().setProjectId(projectId)
                                .setCredentials(GoogleCredentials.getApplicationDefault()).build(),
                            "sparrow-phone-verification"
                        )
                }
                FirebaseAuth.getInstance(app)
            }
            return PhoneVerification(required) { token ->
                withContext(Dispatchers.IO) {
                    val decoded = auth.verifyIdToken(token, true)
                    val firebase = decoded.claims["firebase"] as? Map<*, *>
                    require(firebase?.get("sign_in_provider") == "phone") { "Phone sign-in required" }
                    requireNotNull(decoded.claims["phone_number"] as? String)
                }
            }
        }
    }
}
