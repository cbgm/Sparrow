package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.RecordLocalIdentitySharedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverManualIdentityExchangeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DirectIdentityController(
    private val recoverIdentity: RecoverManualIdentityExchangeUseCase,
    private val recordShared: RecordLocalIdentitySharedUseCase
) {
    private lateinit var scope: CoroutineScope
    private lateinit var contactId: String

    internal fun bind(scope: CoroutineScope, contactId: String) {
        this.scope = scope
        this.contactId = contactId
    }

    fun recoverAfterManualImport(
        conversationContext: Flow<DirectChatContext>,
        onFailure: (Throwable) -> Unit
    ) {
        scope.launch {
            // The use case rechecks persisted state; do not attempt recovery before import.
            val context = conversationContext.first {
                it.conversation != null &&
                    it.setupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING &&
                    it.remoteIdentity?.locallyImported == true
            }
            if (context.remoteIdentity?.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
                recoverIdentity(contactId).onFailure(onFailure)
            }
        }
    }

    fun shareIdentity(
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        scope.launch {
            recordShared(contactId)
                .onSuccess { onSuccess() }
                .onFailure(onFailure)
        }
    }
}
