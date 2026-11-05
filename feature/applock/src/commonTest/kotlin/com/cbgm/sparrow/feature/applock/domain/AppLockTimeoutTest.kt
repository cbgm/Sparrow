package com.cbgm.sparrow.feature.applock.domain

import com.cbgm.sparrow.feature.applock.domain.model.AppLockSession
import com.cbgm.sparrow.feature.applock.domain.model.AppLockSettings
import com.cbgm.sparrow.feature.applock.domain.model.AppLockTimeout
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppHiddenUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppVisibleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLockTimeoutTest {
    @Test
    fun timeoutExpiresAfterItsDuration() {
        assertFalse(AppLockTimeout.FIVE_MINUTES.hasExpired(hiddenAtEpochMilliseconds = 0L, nowEpochMilliseconds = 299_999L))
        assertTrue(AppLockTimeout.FIVE_MINUTES.hasExpired(hiddenAtEpochMilliseconds = 0L, nowEpochMilliseconds = 300_000L))
        assertFalse(AppLockTimeout.ON_RESTART.hasExpired(hiddenAtEpochMilliseconds = 0L, nowEpochMilliseconds = Long.MAX_VALUE))
    }

    @Test
    fun aClockSetBackwardsCountsAsExpired() {
        assertTrue(AppLockTimeout.THIRTY_MINUTES.hasExpired(hiddenAtEpochMilliseconds = 1_000L, nowEpochMilliseconds = 0L))
    }

    @Test
    fun theAppLocksAgainAfterTheTimeoutInTheBackground() =
        runTest {
            val session = AppLockSession().apply { unlock() }
            var now = 0L
            RecordAppHiddenUseCase(repository(AppLockTimeout.FIVE_MINUTES), session) { now }()
            now = 300_000L
            RecordAppVisibleUseCase(session) { now }()

            assertFalse(session.unlocked.value)
        }

    @Test
    fun aShortTripToAnotherAppKeepsItUnlocked() =
        runTest {
            val session = AppLockSession().apply { unlock() }
            var now = 0L
            RecordAppHiddenUseCase(repository(AppLockTimeout.FIVE_MINUTES), session) { now }()
            now = 10_000L
            RecordAppVisibleUseCase(session) { now }()

            assertTrue(session.unlocked.value)
        }

    @Test
    fun goingToTheBackgroundWhileLockedStartsNoTimer() =
        runTest {
            val session = AppLockSession()
            RecordAppHiddenUseCase(repository(AppLockTimeout.IMMEDIATELY), session) { 0L }()
            session.unlock()
            RecordAppVisibleUseCase(session) { 1_000_000L }()

            assertTrue(session.unlocked.value)
        }

    private fun repository(timeout: AppLockTimeout) =
        object : AppLockRepository {
            override fun observeEnabled(): Flow<Boolean> = flowOf(true)

            override fun observeSettings(): Flow<AppLockSettings> = flowOf(AppLockSettings(enabled = true, timeout = timeout))

            override suspend fun setEnabled(enabled: Boolean): Result<Unit> = error("Not used")

            override suspend fun setTimeout(timeout: AppLockTimeout): Result<Unit> = error("Not used")
        }
}
