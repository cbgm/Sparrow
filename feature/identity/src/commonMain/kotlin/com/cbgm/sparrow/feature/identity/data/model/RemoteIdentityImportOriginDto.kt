package com.cbgm.sparrow.feature.identity.data.model

internal enum class RemoteIdentityImportOriginDto {
    LOCAL_IMPORT,
    TRUSTED_QR_IMPORT,
    REMOTE_PACKET,
    REMOTE_HANDSHAKE
}
