package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.feature.transport.blob.isFileTransferFailure
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute

/** What the user reads when a message could not be sent: what happened, never the technical failure. */
internal fun Throwable.sendFailureText(fallback: String): String =
    when {
        isWaitingForRoute() -> "Not sent: there is no protected route right now, and sending directly is switched off."
        isFileTransferFailure() -> "Not sent: the attachment could not be uploaded. Please try again."
        else -> fallback
    }
