package com.cbgm.sparrow.feature.transport.controlplane

/** Expected transient condition: no control plane could complete the request. */
class ControlPlaneUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/** The server was contacted but explicitly rejected the request. This is not an offline event. */
class ControlPlaneRequestRejectedException(
    message: String
) : IllegalStateException(message)
