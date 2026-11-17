package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.BoundedRateLimiter
import com.cbgm.sparrow.server.security.CreationWorkCheck
import com.cbgm.sparrow.server.security.RateLimitPolicy
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

internal data class PresenceRuntime(
    val store: PresenceStorage,
    val phoneVerification: PhoneVerification,
    val aliasClaimKeys: AliasClaimKeys = AliasClaimKeys(),
    val aliasClaimRateLimiter: BoundedRateLimiter = BoundedRateLimiter(ALIAS_CLAIM_RATE_LIMIT),
    /** The price of an invitation route lookup; the app solves 18 bits, like for a mailbox. */
    val lookupWork: CreationWorkCheck = CreationWorkCheck(DEFAULT_LOOKUP_WORK_BITS)
)

/** An app claims its aliases when it starts and every few hours; a device needs only a few claims per window. */
private val ALIAS_CLAIM_RATE_LIMIT = RateLimitPolicy(maximumRequests = 20, windowMilliseconds = 60L * 60L * 1_000L)

internal fun createPresenceRuntime(store: PresenceStorage): PresenceRuntime =
    PresenceRuntime(
        store = store,
        phoneVerification = createPhoneVerification(),
        lookupWork = CreationWorkCheck(ServiceEnvironment.int("PRESENCE_LOOKUP_WORK_BITS", DEFAULT_LOOKUP_WORK_BITS))
    )

internal fun Application.configurePresenceLifecycle(runtime: PresenceRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.store.close()
    }
}

internal fun Application.installPresencePlugins() {
    installServerObservability("presence-directory") { true }
    install(ContentNegotiation) {
        json(serverJson)
    }
}

private const val DEFAULT_LOOKUP_WORK_BITS = 18
