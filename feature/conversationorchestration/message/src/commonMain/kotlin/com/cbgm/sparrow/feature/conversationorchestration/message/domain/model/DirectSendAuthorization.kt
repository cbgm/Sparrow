package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.DirectChatAuthorizationRequiredException

/** What a new direct message to a contact may do right now. */
sealed interface DirectSendAuthorization {
    data object Granted : DirectSendAuthorization

    data class ContactBlocked(
        val reason: String
    ) : DirectSendAuthorization

    /**
     * The previous exchange can remain MUTUAL until the user reviews the new identity. Never encrypt for
     * those obsolete keys; the message waits.
     */
    data object IdentityChangePending : DirectSendAuthorization

    /** Authorization or keys are missing; a fresh invitation may fix it. */
    data class Missing(
        val error: DirectChatAuthorizationRequiredException
    ) : DirectSendAuthorization
}
