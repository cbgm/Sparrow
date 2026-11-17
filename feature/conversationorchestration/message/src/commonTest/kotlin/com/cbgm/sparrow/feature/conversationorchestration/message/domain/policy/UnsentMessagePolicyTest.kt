package com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy

import com.cbgm.sparrow.feature.transport.blob.FileTransferFailedException
import com.cbgm.sparrow.feature.transport.circuit.ProtectedRouteMissingException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnsentMessagePolicyTest {
    @Test
    fun aMessageWaitingForAProtectedRouteOrAnUploadIsKept() {
        assertTrue(UnsentMessagePolicy.keepsMessage(ProtectedRouteMissingException()))
        assertTrue(UnsentMessagePolicy.keepsMessage(IllegalStateException("wrapped", FileTransferFailedException(IllegalStateException("refused")))))
    }

    @Test
    fun aMessageThatCannotBeBuiltIsNotKept() {
        assertFalse(UnsentMessagePolicy.keepsMessage(IllegalArgumentException("Attachment is too large")))
    }
}
