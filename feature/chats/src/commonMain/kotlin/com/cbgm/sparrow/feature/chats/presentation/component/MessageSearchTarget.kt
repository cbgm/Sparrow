package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal data class MessageSearchTargetState(
    val highlightedMessageId: String?,
    val isHandled: Boolean
)

@Composable
internal fun rememberMessageSearchTargetState(
    targetMessageId: String?,
    messageIds: List<String>,
    listState: LazyListState
): MessageSearchTargetState {
    var isHandled by remember(targetMessageId) { mutableStateOf(targetMessageId == null) }
    var highlightedMessageId by remember(targetMessageId) { mutableStateOf<String?>(null) }

    LaunchedEffect(targetMessageId, messageIds) {
        if (isHandled || targetMessageId == null) return@LaunchedEffect

        val targetIndex = messageIds.indexOf(targetMessageId)
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            highlightedMessageId = targetMessageId
        }
        isHandled = true
    }

    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId == null) return@LaunchedEffect
        delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)
        highlightedMessageId = null
    }

    return MessageSearchTargetState(
        highlightedMessageId = highlightedMessageId,
        isHandled = isHandled
    )
}

private const val HIGHLIGHT_DURATION_MILLIS = 2_000L
