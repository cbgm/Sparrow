package com.cbgm.sparrow.feature.identity.domain.model

/** Whether a sign-up may send its SMS today, asked before the SMS is sent. */
sealed interface PhoneVerificationSlotStatus {
    /** A slot is kept for this sign-up; the code may be sent [resends] more times. */
    data class Reserved(
        val resends: Int
    ) : PhoneVerificationSlotStatus

    /** Today's sign-ups are used up; the next day starts at [retryAtEpochMilliseconds]. No SMS is sent. */
    data class DayFull(
        val retryAtEpochMilliseconds: Long
    ) : PhoneVerificationSlotStatus
}
