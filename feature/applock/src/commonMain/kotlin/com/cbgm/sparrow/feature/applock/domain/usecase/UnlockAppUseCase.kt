package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession

/** Marks this process as unlocked, after authentication or when no lock is set. */
class UnlockAppUseCase(
    private val session: AppLockSession
) {
    operator fun invoke() = session.unlock()
}
