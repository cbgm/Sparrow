package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Looks up the invitation route of a phone alias or a device routing ID at the user's Control Planes, one after the
 * other, through the relays of the Control Plane client ([com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayEngine]):
 * the presence directory does not see who asks. Every lookup is paid with the mailbox proof of work.
 */
internal class InvitationRouteRemoteDataSource(
    private val controlPlaneHttpClient: HttpClient,
    private val controlPlaneConfiguration: ControlPlaneEndpointStore,
    private val work: MailboxCreationWorkSolver,
    private val json: Json
) {
    /**
     * The route of the first Control Plane that knows [routingId]; null when every one answered that it has none.
     * Fails when none knows it and at least one could not be asked.
     */
    suspend fun find(routingId: String): Result<InvitationRouteResultDto?> =
        safeSuspendCall(logFailure = false) {
            var failure: Throwable? = null
            controlPlaneConfiguration.endpoints.value.distinctBy(ControlPlaneEndpoint::baseUrl).forEach { endpoint ->
                when (val answer = ask(endpoint.baseUrl, routingId)) {
                    is Answer.Found -> return@safeSuspendCall answer.result
                    is Answer.Failed -> failure = answer.cause
                    Answer.Unknown -> Unit
                }
            }
            failure?.let { throw it }
            null
        }

    private suspend fun ask(
        baseUrl: String,
        routingId: String
    ): Answer =
        try {
            val body =
                controlPlaneHttpClient
                    .post("${baseUrl.trimEnd('/')}$PATH") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(InvitationRouteLookupDto(routingId, work.solve())))
                    }.bodyAsText()
            Answer.Found(json.decodeFromString<InvitationRouteResultDto>(body))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refusal: ClientRequestException) {
            Answer.Unknown.takeIf { refusal.response.status == HttpStatusCode.NotFound } ?: Answer.Failed(refusal)
        } catch (failure: Exception) {
            Answer.Failed(failure)
        }

    private sealed interface Answer {
        class Found(
            val result: InvitationRouteResultDto
        ) : Answer

        data object Unknown : Answer

        class Failed(
            val cause: Throwable
        ) : Answer
    }

    private companion object {
        const val PATH = "/v1/invitation-routes"
    }
}
