package com.cbgm.sparrow.feature.identity.domain.model

enum class RemoteIdentityOrigin {
    LOCAL_IMPORT,
    TRUSTED_QR_IMPORT,
    REMOTE_PACKET,
    REMOTE_HANDSHAKE
}
