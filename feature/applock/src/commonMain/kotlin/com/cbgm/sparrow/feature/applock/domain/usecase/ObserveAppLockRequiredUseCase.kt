package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** True while the app lock is on and this process is not unlocked. */
class ObserveAppLockRequiredUseCase(
    private val repository: AppLockRepository,
    private val session: AppLockSession
) {
    operator fun invoke(): Flow<Boolean> =
        combine(repository.observeEnabled(), session.unlocked) { enabled, unlocked -> enabled && !unlocked }
            .distinctUntilChanged()
}
