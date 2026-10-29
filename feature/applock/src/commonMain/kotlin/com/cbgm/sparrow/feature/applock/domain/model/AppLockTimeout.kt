package com.cbgm.sparrow.feature.applock.domain.model

/** How long the app may stay in the background before it locks again. [ON_RESTART] locks only on a new start. */
enum class AppLockTimeout(
    private val milliseconds: Long?
) {
    // A few seconds, so turning the screen or a configuration change does not count as leaving the app.
    IMMEDIATELY(2_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(5L * 60_000L),
    THIRTY_MINUTES(30L * 60_000L),
    ON_RESTART(null);

    /** A clock that went backwards counts as expired, so changing the time cannot keep the app open. */
    fun hasExpired(
        hiddenAtEpochMilliseconds: Long,
        nowEpochMilliseconds: Long
    ): Boolean {
        val elapsed = nowEpochMilliseconds - hiddenAtEpochMilliseconds
        return milliseconds?.let { limit -> elapsed < 0L || elapsed >= limit } ?: false
    }

    companion object {
        val DEFAULT = FIVE_MINUTES
    }
}

data class AppLockSettings(
    val enabled: Boolean,
    val timeout: AppLockTimeout
)
