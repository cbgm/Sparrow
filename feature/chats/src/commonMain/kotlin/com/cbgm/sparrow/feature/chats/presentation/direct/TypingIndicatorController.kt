package com.cbgm.sparrow.feature.chats.presentation.direct

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class TypingIndicatorController(
    private val scope: CoroutineScope,
    private val sendTypingState: suspend (Boolean) -> Result<Unit>,
    logTag: String,
    private val localTimeout: Duration = 1500.milliseconds,
    private val remoteTimeout: Duration = 3000.milliseconds
) {
    private val logger = SparrowLog.withTag(logTag)

    private val _isContactTyping = MutableStateFlow(false)
    val isContactTyping: StateFlow<Boolean> = _isContactTyping.asStateFlow()

    private var localStopJob: Job? = null
    private var remoteTimeoutJob: Job? = null
    private var isLocalTyping = false

    fun onIncomingTypingChanged(isTyping: Boolean) {
        remoteTimeoutJob?.cancel()
        _isContactTyping.value = isTyping
        if (isTyping) {
            remoteTimeoutJob = scope.launch {
                delay(remoteTimeout)
                _isContactTyping.value = false
            }
        }
    }

    fun onLocalTextChanged(value: String, sendsIndicators: Boolean) {
        localStopJob?.cancel()

        if (value.isBlank()) {
            stopLocalTyping()
            return
        }

        if (!sendsIndicators) {
            isLocalTyping = false
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            scope.launch { sendNow(isTyping = true) }
        }

        localStopJob = scope.launch {
            delay(localTimeout)
            stopLocalTypingNow()
        }
    }

    fun stopLocalTyping() {
        localStopJob?.cancel()
        localStopJob = null
        if (!isLocalTyping) return
        isLocalTyping = false
        scope.launch { sendNow(isTyping = false) }
    }

    suspend fun stopLocalTypingNow() {
        if (!isLocalTyping) return
        isLocalTyping = false
        sendNow(isTyping = false)
    }

    private suspend fun sendNow(isTyping: Boolean) {
        sendTypingState(isTyping)
            .onFailure { error -> logger.warn(error) { "Could not send typing state" } }
    }
}
