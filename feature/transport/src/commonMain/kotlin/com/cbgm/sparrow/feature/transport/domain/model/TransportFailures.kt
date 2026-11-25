package com.cbgm.sparrow.feature.transport.domain.model

import com.cbgm.sparrow.core.util.result.ExpectedFailure

/** A file could not be sent or fetched: the blob node refused it, or every attempt through a new circuit failed. */
class FileTransferFailedException(
    cause: Throwable
) : IllegalStateException("File transfer failed: ${cause.message}", cause)

/** Nothing left the device: sending directly is switched off and no circuit through enough hops is up. */
class ProtectedRouteMissingException :
    IllegalStateException(
        "No circuit through at least ${CircuitHopCount.PROTECTING} hops; sending directly is switched off"
    )

/** The letter can never be accepted; its envelope is acknowledged and dropped. */
class InvalidInvitationLetterException(
    message: String
) : IllegalArgumentException(message)

/** None of the user's Control Planes knows an invitation mailbox of the recipient. */
class InvitationRouteMissingException : IllegalStateException("The recipient has no invitation mailbox at any Control Plane")

/**
 * The node holding a mailbox is down or unreachable (no connection, or a 5xx from its proxy). Expected
 * while a node is offline: the mailbox is read again on the next sync, and provisioning replaces it
 * once the node leaves the verified directory.
 */
class MailboxNodeUnavailableException(
    cause: Throwable
) : IllegalStateException("Mailbox node is unavailable", cause),
    ExpectedFailure

/** The node no longer knows this mailbox (it expired, was revoked, or the node was installed anew). */
class MailboxGoneException(
    statusCode: Int
) : IllegalStateException("Mailbox is gone ($statusCode)")

/** Expected transient condition: no control plane could complete the request. */
class ControlPlaneUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/** The server was contacted but explicitly rejected the request. This is not an offline event. */
class ControlPlaneRequestRejectedException(
    message: String
) : IllegalStateException(message)
