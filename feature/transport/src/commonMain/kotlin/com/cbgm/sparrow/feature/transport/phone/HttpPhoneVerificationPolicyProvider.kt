package com.cbgm.sparrow.feature.transport.phone

import com.cbgm.sparrow.core.phone.PhoneVerificationPolicyProvider
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/** Control-plane proxy forwards this endpoint to the presence-directory server. */
class HttpPhoneVerificationPolicyProvider(
    private val httpClient: HttpClient,
    private val controlPlanes: ControlPlaneConfiguration
) : PhoneVerificationPolicyProvider {
    override suspend fun isRequired(): Result<Boolean> {
        val endpoints = buildList {
            controlPlanes.activeEndpoint.value?.let(::add)
            controlPlanes.orderedEndpoints().forEach { if (it !in this) add(it) }
        }
        if (endpoints.isEmpty()) {
            return Result.failure(IllegalStateException("No control plane is configured"))
        }
        var lastFailure: Throwable = IllegalStateException("Phone verification policy is unavailable")
        for (endpoint in endpoints) {
            try {
                val policy = withTimeoutOrNull(8_000L) {
                    httpClient.get("${endpoint.baseUrl.trimEnd('/')}/v1/phone-verification/policy")
                        .body<PhoneVerificationPolicyResponse>()
                } ?: error("Phone verification policy request timed out")
                return Result.success(policy.required)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastFailure = error
            }
        }
        return Result.failure(lastFailure)
    }
}

@Serializable
private data class PhoneVerificationPolicyResponse(
    val required: Boolean
)
