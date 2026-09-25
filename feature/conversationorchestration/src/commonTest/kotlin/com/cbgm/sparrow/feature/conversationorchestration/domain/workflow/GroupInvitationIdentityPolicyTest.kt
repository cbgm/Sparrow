package com.cbgm.sparrow.feature.conversationorchestration.domain.workflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupInvitationIdentityPolicyTest {
    private val encryption = ByteArray(32) { 1 }
    private val signing = ByteArray(32) { 2 }

    @Test
    fun newContactMayStageUntrustedGroupKeys() {
        assertEquals(
            GroupInvitationIdentityDisposition.FIRST_CONTACT,
            GroupInvitationIdentityPolicy.evaluate(null, null, encryption, signing)
        )
    }

    @Test
    fun unchangedContactDoesNotTriggerKeyReplacement() {
        assertEquals(
            GroupInvitationIdentityDisposition.MATCHES_STORED_IDENTITY,
            GroupInvitationIdentityPolicy.evaluate(encryption.copyOf(), signing.copyOf(), encryption, signing)
        )
    }

    @Test
    fun changedSigningKeyRequiresApproval() {
        assertEquals(
            GroupInvitationIdentityDisposition.REQUIRES_REVIEW,
            GroupInvitationIdentityPolicy.evaluate(encryption, signing, encryption, ByteArray(32) { 3 })
        )
    }

    @Test
    fun changedEncryptionKeyAlsoRequiresApproval() {
        assertEquals(
            GroupInvitationIdentityDisposition.REQUIRES_REVIEW,
            GroupInvitationIdentityPolicy.evaluate(encryption, signing, ByteArray(32) { 4 }, signing)
        )
    }

    @Test
    fun incompleteIdentityAndInvalidOfferFailClosed() {
        assertFailsWith<IllegalStateException> {
            GroupInvitationIdentityPolicy.evaluate(encryption, null, encryption, signing)
        }
        assertFailsWith<IllegalArgumentException> {
            GroupInvitationIdentityPolicy.evaluate(null, null, byteArrayOf(1), signing)
        }
    }
}
