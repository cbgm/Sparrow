package com.cbgm.sparrow.feature.conversationorchestration.domain.error

/**
 * A signed invitation presents keys different from those already associated with
 * an existing contact. Neither the sender's phone-number label nor their new
 * signature proves continuity with the previously trusted identity.
 *
 * The contact/conversation must remain unchanged until the user explicitly
 * verifies and accepts a separate identity-replacement operation.
 */
class RemoteIdentityReplacementRequiredException(
    val peerId: String
) : IllegalStateException("Contact identity changed; explicit identity recovery is required")
