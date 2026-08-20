package com.cbgm.sparrow.feature.safety.domain.resolver

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageSafetyRiskResolverTest {
    private val resolver = MessageSafetyRiskResolver()

    @Test
    fun credentialPlusSuspiciousLinkIsHighRisk() {
        val result =
            resolver(
                setOf(
                    MessageSafetyReason.CREDENTIAL_REQUEST,
                    MessageSafetyReason.SUSPICIOUS_LINK,
                    MessageSafetyReason.IP_ADDRESS_LINK
                )
            )

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
    }

    @Test
    fun urgentPaymentRequestIsHighRisk() {
        val result =
            resolver(
                setOf(
                    MessageSafetyReason.PAYMENT_REQUEST,
                    MessageSafetyReason.URGENT_ACTION_REQUEST
                )
            )

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
    }
}
