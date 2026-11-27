package com.cbgm.sparrow.feature.identity.presentation.verification

import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStageUi
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneVerificationUiStateTest {
    @Test
    fun theCodeCanBeSentAgainOnlyWhileTheVerifierAllowsIt() {
        val waiting = PhoneVerificationUiState(stage = PhoneVerificationStageUi.CODE, resendsLeft = 1)

        assertTrue(waiting.canResend)
        assertFalse(waiting.copy(resendsLeft = 0).canResend)
        assertFalse(waiting.copy(resendInSeconds = 30).canResend)
    }

    @Test
    fun aFullDayOffersNeitherVerifyNorSendAgain() {
        val full = PhoneVerificationUiState(stage = PhoneVerificationStageUi.DAY_FULL, resendsLeft = 1)

        assertFalse(full.canConfirm)
        assertFalse(full.canResend)
    }
}
