package com.cbgm.sparrow.feature.polls.presentation.message

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PollMessageViewModel(
    part: PollUi
) : BaseViewModel() {
    private val _isExpired = MutableStateFlow(
        part.expiresAtEpochMilliseconds?.let { it <= SystemClock.nowEpochMilliseconds() } == true
    )
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private val _isVotersOverlayVisible = MutableStateFlow(false)
    val isVotersOverlayVisible: StateFlow<Boolean> = _isVotersOverlayVisible.asStateFlow()

    private var expiryJob: Job? = null

    init {
        scheduleExpiry(part.expiresAtEpochMilliseconds)
    }

    private fun scheduleExpiry(expiresAt: Long?) {
        expiryJob?.cancel()
        if (expiresAt == null || _isExpired.value) return
        expiryJob = viewModelScope.launch {
            delay((expiresAt - SystemClock.nowEpochMilliseconds()).coerceAtLeast(0L).milliseconds)
            _isExpired.value = true
        }
    }

    fun openVoters() {
        _isVotersOverlayVisible.value = true
    }

    fun dismissVoters() {
        _isVotersOverlayVisible.value = false
    }
}
