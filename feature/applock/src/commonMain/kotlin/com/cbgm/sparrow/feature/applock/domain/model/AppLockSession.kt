package com.cbgm.sparrow.feature.applock.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether this process has been unlocked. It lives in memory only, so every new process (also one Android
 * restarted in the background) starts locked.
 */
class AppLockSession {
    private val mutableUnlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = mutableUnlocked.asStateFlow()

    // When the unlocked app went to the background and the timeout that applied then.
    private var hiddenAtEpochMilliseconds: Long? = null
    private var timeout: AppLockTimeout? = null

    fun unlock() {
        hiddenAtEpochMilliseconds = null
        mutableUnlocked.value = true
    }

    fun hidden(
        atEpochMilliseconds: Long,
        timeout: AppLockTimeout
    ) {
        if (!unlocked.value) return
        hiddenAtEpochMilliseconds = atEpochMilliseconds
        this.timeout = timeout
    }

    /** Locks right away when the app was hidden for at least the timeout; decided before anything is shown. */
    fun visible(nowEpochMilliseconds: Long) {
        val hiddenAt = hiddenAtEpochMilliseconds ?: return
        hiddenAtEpochMilliseconds = null
        if (timeout?.hasExpired(hiddenAt, nowEpochMilliseconds) == true) mutableUnlocked.value = false
    }
}
