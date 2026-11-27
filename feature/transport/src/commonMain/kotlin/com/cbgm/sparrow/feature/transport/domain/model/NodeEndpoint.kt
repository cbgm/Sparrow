package com.cbgm.sparrow.feature.transport.domain.model

data class NodeEndpoint(
    val nodeId: String,
    val websocketUrl: String,
    val mailboxRouteEndpoint: String? = null,
    val mailboxAccessEndpoint: String? = null
)
