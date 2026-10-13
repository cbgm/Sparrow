package com.cbgm.sparrow.feature.chats.presentation.direct

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class IndicatorController(
    private val scope: CoroutineScope,
    private val sendIndicatorState: suspend (ConversationIndicator) -> Result<Unit>,
    logTag: String,
    private val indicatorTimeout: Duration = 1500.milliseconds,
    private val heartbeatInterval: Duration = 1500.milliseconds,
    private val remoteTimeout: Duration = 3000.milliseconds
) {
    private val logger = SparrowLog.withTag(logTag)

    private val _remoteIndicatorType = MutableStateFlow(ConversationIndicator.NONE)
    val remoteIndicatorType: StateFlow<ConversationIndicator> = _remoteIndicatorType.asStateFlow()

    private var localIndicatorType = ConversationIndicator.NONE
    private var indicatorStopJob: Job? = null
    private var heartbeatJob: Job? = null
    private var remoteTimeoutJob: Job? = null

    fun onIncomingIndicatorChanged(indicatorType: ConversationIndicator) {
        remoteTimeoutJob?.cancel()
        _remoteIndicatorType.value = indicatorType
        if (indicatorType != ConversationIndicator.NONE) {
            remoteTimeoutJob = scope.launch {
                delay(remoteTimeout)
                _remoteIndicatorType.value = ConversationIndicator.NONE
            }
        }
    }

    fun onLocalTextChanged(
        value: String,
        sendsIndicators: Boolean
    ) {
        indicatorStopJob?.cancel()

        if (value.isBlank()) {
            stopLocalTyping()
            return
        }

        if (!sendsIndicators || localIndicatorType == ConversationIndicator.VOICE) {
            return
        }

        setLocalIndicator(ConversationIndicator.TYPING)
        indicatorStopJob = scope.launch {
            delay(indicatorTimeout)
            stopLocalIndicator()
        }
    }

    fun onLocalVoiceRecordingChanged(
        isRecording: Boolean,
        sendsIndicators: Boolean
    ) {
        if (isRecording) {
            if (sendsIndicators) {
                setLocalIndicator(ConversationIndicator.VOICE)
            }
        } else if (localIndicatorType == ConversationIndicator.VOICE) {
            setLocalIndicator(ConversationIndicator.NONE)
        }
    }

    fun stopLocalTyping() {
        indicatorStopJob?.cancel()
        indicatorStopJob = null
        if (localIndicatorType != ConversationIndicator.TYPING) return
        setLocalIndicator(ConversationIndicator.NONE)
    }

    fun stopLocalIndicator() {
        indicatorStopJob?.cancel()
        indicatorStopJob = null
        if (localIndicatorType == ConversationIndicator.NONE) return
        setLocalIndicator(ConversationIndicator.NONE)
    }

    private fun setLocalIndicator(indicatorType: ConversationIndicator) {
        if (localIndicatorType == indicatorType) return
        localIndicatorType = indicatorType
        restartHeartbeat(indicatorType)
        scope.launch { sendNow(indicatorType) }
    }

    private fun restartHeartbeat(indicatorType: ConversationIndicator) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        if (indicatorType == ConversationIndicator.NONE) return

        heartbeatJob = scope.launch {
            while (true) {
                delay(heartbeatInterval)
                sendNow(indicatorType)
            }
        }
    }

    private suspend fun sendNow(indicatorType: ConversationIndicator) {
        sendIndicatorState(indicatorType)
            .onFailure { error -> logger.error(error) { "Could not send indicator state" } }
    }
}
