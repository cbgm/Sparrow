package com.cbgm.sparrow.feature.chats.presentation.direct

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
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
    private val sendIndicatorState: suspend (IndicatorType) -> Result<Unit>,
    logTag: String,
    private val indicatorTimeout: Duration = 1500.milliseconds,
    private val heartbeatInterval: Duration = 1500.milliseconds,
    private val remoteTimeout: Duration = 3000.milliseconds
) {
    private val logger = SparrowLog.withTag(logTag)

    private val _remoteIndicatorType = MutableStateFlow(IndicatorType.NONE)
    val remoteIndicatorType: StateFlow<IndicatorType> = _remoteIndicatorType.asStateFlow()

    private var localIndicatorType = IndicatorType.NONE
    private var indicatorStopJob: Job? = null
    private var heartbeatJob: Job? = null
    private var remoteTimeoutJob: Job? = null

    fun onIncomingIndicatorChanged(indicatorType: IndicatorType) {
        remoteTimeoutJob?.cancel()
        _remoteIndicatorType.value = indicatorType
        if (indicatorType != IndicatorType.NONE) {
            remoteTimeoutJob = scope.launch {
                delay(remoteTimeout)
                _remoteIndicatorType.value = IndicatorType.NONE
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

        if (!sendsIndicators || localIndicatorType == IndicatorType.VOICE) {
            return
        }

        setLocalIndicator(IndicatorType.TYPING)
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
                setLocalIndicator(IndicatorType.VOICE)
            }
        } else if (localIndicatorType == IndicatorType.VOICE) {
            setLocalIndicator(IndicatorType.NONE)
        }
    }

    fun stopLocalTyping() {
        indicatorStopJob?.cancel()
        indicatorStopJob = null
        if (localIndicatorType != IndicatorType.TYPING) return
        setLocalIndicator(IndicatorType.NONE)
    }

    fun stopLocalIndicator() {
        indicatorStopJob?.cancel()
        indicatorStopJob = null
        if (localIndicatorType == IndicatorType.NONE) return
        setLocalIndicator(IndicatorType.NONE)
    }

    private fun setLocalIndicator(indicatorType: IndicatorType) {
        if (localIndicatorType == indicatorType) return
        localIndicatorType = indicatorType
        restartHeartbeat(indicatorType)
        scope.launch { sendNow(indicatorType) }
    }

    private fun restartHeartbeat(indicatorType: IndicatorType) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        if (indicatorType == IndicatorType.NONE) return

        heartbeatJob = scope.launch {
            while (true) {
                delay(heartbeatInterval)
                sendNow(indicatorType)
            }
        }
    }

    private suspend fun sendNow(indicatorType: IndicatorType) {
        sendIndicatorState(indicatorType)
            .onFailure { error -> logger.error(error) { "Could not send indicator state" } }
    }
}
