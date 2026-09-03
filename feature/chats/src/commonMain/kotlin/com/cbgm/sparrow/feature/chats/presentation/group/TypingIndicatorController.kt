package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.core.logging.SparrowLog
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

internal class GroupTypingController(
    private val scope: CoroutineScope,
    private val observeMemberTyping: (contactId: String) -> Flow<Boolean>,
    private val sendTypingState: suspend (Boolean) -> Result<Unit>,
    logTag: String,
    private val localTimeout: Duration = 1500.milliseconds,
    private val remoteTimeout: Duration = 3000.milliseconds
) {
    private val logger = SparrowLog.withTag(logTag)

    private val _typingContactIds = MutableStateFlow<Set<String>>(emptySet())
    val typingContactIds: StateFlow<Set<String>> = _typingContactIds.asStateFlow()

    private val observerJobs = mutableMapOf<String, Job>()
    private val timeoutJobs = mutableMapOf<String, Job>()
    private var localStopJob: Job? = null
    private var isLocalTyping = false

    /** Begin following [memberContactIds]; observers for members are added/removed as it changes. */
    fun start(memberContactIds: Flow<Set<String>>) {
        scope.launch {
            memberContactIds.distinctUntilChanged().collect(::updateObservers)
        }
    }

    fun onLocalTextChanged(value: String) {
        localStopJob?.cancel()

        if (value.isBlank()) {
            stopLocalTyping()
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

    private fun updateObservers(contactIds: Set<String>) {
        removeObservers(observerJobs.keys - contactIds)
        (contactIds - observerJobs.keys).forEach(::observeMember)
    }

    private fun removeObservers(contactIds: Set<String>) {
        contactIds.forEach { contactId ->
            observerJobs.remove(contactId)?.cancel()
            timeoutJobs.remove(contactId)?.cancel()
            _typingContactIds.update { it - contactId }
        }
    }

    private fun observeMember(contactId: String) {
        observerJobs[contactId] =
            scope.launch {
                observeMemberTyping(contactId).collect { isTyping -> updateRemoteTyping(contactId, isTyping) }
            }
    }

    private fun updateRemoteTyping(contactId: String, isTyping: Boolean) {
        timeoutJobs.remove(contactId)?.cancel()
        _typingContactIds.update { current -> if (isTyping) current + contactId else current - contactId }
        if (!isTyping) return

        timeoutJobs[contactId] =
            scope.launch {
                delay(remoteTimeout)
                _typingContactIds.update { it - contactId }
            }
    }

    private suspend fun sendNow(isTyping: Boolean) {
        sendTypingState(isTyping)
            .onFailure { error -> logger.warn(error) { "Could not send group typing state" } }
    }
}
