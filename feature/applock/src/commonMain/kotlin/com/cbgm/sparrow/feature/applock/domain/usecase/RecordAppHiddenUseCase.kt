package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.first

/** Notes when the unlocked app went to the background and which timeout applies; a locked app has nothing to time. */
class RecordAppHiddenUseCase(
    private val repository: AppLockRepository,
    private val session: AppLockSession,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    suspend operator fun invoke(): Result<Unit> =
        safeSuspendCall {
            val hiddenAt = now()
            val settings = repository.observeSettings().first()
            if (settings.enabled) session.hidden(hiddenAt, settings.timeout)
        }
}
