package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState

data class DirectChatContext(
    val conversation: DirectConversation?,
    val contact: Contact?,
    val handshake: IdentityHandshakeState?,
    val setupMode: DirectIdentitySetupMode
)
