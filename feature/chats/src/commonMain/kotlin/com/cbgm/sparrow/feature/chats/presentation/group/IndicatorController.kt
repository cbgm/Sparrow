package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class IndicatorController(
    private val scope: CoroutineScope,
    private val observeMemberIndicator: (contactId: String) -> Flow<IndicatorType>,
    private val sendIndicatorState: suspend (IndicatorType) -> Result<Unit>,
    logTag: String,
    private val indicatorTimeout: Duration = 1500.milliseconds,
    private val heartbeatInterval: Duration = 1500.milliseconds,
    private val remoteTimeout: Duration = 3000.milliseconds
) {
    private val logger = SparrowLog.withTag(logTag)

    private val _memberIndicators = MutableStateFlow<Map<String, IndicatorType>>(emptyMap())
    val memberIndicators: StateFlow<Map<String, IndicatorType>> = _memberIndicators.asStateFlow()

    private val observerJobs = mutableMapOf<String, Job>()
    private val timeoutJobs = mutableMapOf<String, Job>()
    private var localIndicatorType = IndicatorType.NONE
    private var indicatorStopJob: Job? = null
    private var heartbeatJob: Job? = null

    fun start(memberContactIds: Flow<Set<String>>) {
        scope.launch {
            memberContactIds.distinctUntilChanged().collect(::updateObservers)
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

    private fun updateObservers(contactIds: Set<String>) {
        removeObservers(observerJobs.keys - contactIds)
        (contactIds - observerJobs.keys).forEach(::observeMember)
    }

    private fun removeObservers(contactIds: Set<String>) {
        contactIds.forEach { contactId ->
            observerJobs.remove(contactId)?.cancel()
            timeoutJobs.remove(contactId)?.cancel()
            _memberIndicators.update { it - contactId }
        }
    }

    private fun observeMember(contactId: String) {
        observerJobs[contactId] =
            scope.launch {
                observeMemberIndicator(contactId).collect { indicatorType ->
                    updateRemoteIndicator(contactId, indicatorType)
                }
            }
    }

    private fun updateRemoteIndicator(
        contactId: String,
        indicatorType: IndicatorType
    ) {
        timeoutJobs.remove(contactId)?.cancel()
        _memberIndicators.update { current ->
            if (indicatorType == IndicatorType.NONE) {
                current - contactId
            } else {
                current + (contactId to indicatorType)
            }
        }
        if (indicatorType == IndicatorType.NONE) return

        timeoutJobs[contactId] =
            scope.launch {
                delay(remoteTimeout)
                _memberIndicators.update { it - contactId }
            }
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
            .onFailure { error -> logger.error(error) { "Could not send group indicator state" } }
    }
}
