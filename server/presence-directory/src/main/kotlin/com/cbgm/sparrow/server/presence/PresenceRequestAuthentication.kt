package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.security.NodeRequestAuthentication
import com.cbgm.sparrow.server.security.NodeRequestHeaders
import io.ktor.server.application.ApplicationCall

internal fun ApplicationCall.requestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID]
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull()
    val nonce = request.headers[NodeRequestHeaders.NONCE]
    val signature = request.headers[NodeRequestHeaders.SIGNATURE]

    return nodeId?.let { resolvedNodeId ->
        timestamp?.let { resolvedTimestamp ->
            nonce?.let { resolvedNonce ->
                signature?.let { resolvedSignature ->
                    NodeRequestAuthentication(
                        nodeId = resolvedNodeId,
                        timestampEpochMilliseconds = resolvedTimestamp,
                        nonce = resolvedNonce,
                        signature = resolvedSignature
                    )
                }
            }
        }
    }
}
