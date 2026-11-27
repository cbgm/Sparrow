package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession

/**
 * Locks the app again when it was in the background for at least the chosen timeout. Runs synchronously
 * when the app becomes visible, so nothing (such as a notification) can open a screen before the lock.
 */
class RecordAppVisibleUseCase(
    private val session: AppLockSession,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    operator fun invoke() = session.visible(now())
}
