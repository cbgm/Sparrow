package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResolution
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.protocol.identity.LocalIdentityChangeHandler
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Decides at startup what to do with the local identity. Nothing is deleted on a read error: the key
 * store can be briefly unavailable (for example right after a reboot), so reading is tried again and a
 * lasting error fails startup instead. An identity that is positively broken is only reset silently when
 * there are no chats to lose; otherwise the user decides.
 */
class ResolveIdentityStatusUseCase(
    private val identityRepository: IdentityRepository,
    private val localIdentityChangeHandler: LocalIdentityChangeHandler,
    private val recoverIncompleteIdentity: RecoverIncompleteIdentityUseCase,
    private val retryDelayMilliseconds: Long = DEFAULT_RETRY_DELAY_MILLISECONDS
) {
    suspend operator fun invoke(): Result<IdentityResolution> =
        safeSuspendCall {
            when (readStatus()) {
                IdentityStatus.READY -> IdentityResolution.READY
                IdentityStatus.NOT_CREATED -> IdentityResolution.NOT_CREATED
                IdentityStatus.INCOMPLETE -> resolveIncomplete()
            }
        }

    private suspend fun readStatus(): IdentityStatus {
        repeat(ATTEMPTS - 1) { attempt ->
            identityRepository.getStatus().onSuccess { status -> return status }
            delay((retryDelayMilliseconds * (attempt + 1)).milliseconds)
        }
        return identityRepository.getStatus().getOrThrow()
    }

    private suspend fun resolveIncomplete(): IdentityResolution =
        if (localIdentityChangeHandler.hasConversations().getOrThrow()) {
            IdentityResolution.KEYS_UNUSABLE
        } else {
            recoverIncompleteIdentity().getOrThrow()
            IdentityResolution.NOT_CREATED
        }

    private companion object {
        const val ATTEMPTS = 3
        const val DEFAULT_RETRY_DELAY_MILLISECONDS = 1_000L
    }
}
