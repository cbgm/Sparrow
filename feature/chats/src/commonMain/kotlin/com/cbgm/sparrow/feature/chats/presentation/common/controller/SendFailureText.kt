package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.feature.transport.blob.isFileTransferFailure
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute

/** What the user reads when a message could not be sent: what happened, never the technical failure. */
internal fun Throwable.sendFailureText(fallback: String): String =
    when {
        isWaitingForRoute() ->
            "Not sent yet: there is no protected route right now, and sending directly is switched off. " +
                "The message is kept and sent once a route is back."
        isFileTransferFailure() ->
            "Not sent yet: the attachment could not be uploaded. The message is kept and sent again later, or retry it now."
        else -> fallback
    }
