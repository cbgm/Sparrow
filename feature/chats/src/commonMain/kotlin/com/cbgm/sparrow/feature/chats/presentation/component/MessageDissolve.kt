package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class DissolvingMessageListState<T>(
    initialMessages: List<T>,
    private val idOf: (T) -> String
) {
    val visibleMessages = mutableStateListOf<T>().apply { addAll(initialMessages) }
    private val dissolvingIds = mutableStateMapOf<String, Boolean>()

    val messages: List<T>
        get() = visibleMessages

    fun update(incomingMessages: List<T>): List<String> {
        val incomingById = incomingMessages.associateBy(idOf)
        val newlyDissolvingIds = mutableListOf<String>()

        val currentVisibleIds = visibleMessages.map(idOf).toSet()
        val missingIds = currentVisibleIds - incomingById.keys

        visibleMessages.indices.forEach { index ->
            val id = idOf(visibleMessages[index])
            incomingById[id]?.let { current ->
                visibleMessages[index] = current
                dissolvingIds.remove(id)
            }
        }

        missingIds.forEach { id ->
            if (dissolvingIds[id] != true) {
                dissolvingIds[id] = true
                newlyDissolvingIds += id
            }
        }

        val visibleIdsSet = visibleMessages.mapTo(mutableSetOf(), idOf)
        incomingMessages
            .filterNot { idOf(it) in visibleIdsSet }
            .asReversed()
            .forEach { visibleMessages.add(0, it) }

        return newlyDissolvingIds
    }

    fun isDissolving(message: T): Boolean = dissolvingIds[idOf(message)] == true

    fun finishDissolve(messageId: String) {
        if (dissolvingIds.remove(messageId) == true) {
            visibleMessages.removeAll { item -> idOf(item) == messageId }
        }
    }
}

@Composable
internal fun <T> rememberDissolvingMessageListState(
    messages: List<T>,
    idOf: (T) -> String
): DissolvingMessageListState<T> {
    val state = remember { DissolvingMessageListState(messages, idOf) }
    val duration = remember { Dimens.MessageDeletion.dissolveDurationMillis.milliseconds }

    LaunchedEffect(messages) {
        state.update(messages).forEach { messageId ->
            launch {
                delay(duration)
                state.finishDissolve(messageId)
            }
        }
    }
    return state
}

@Composable
internal fun MessageDissolve(
    isDissolving: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val d = remember { Dimens.MessageDeletion }
    val duration = remember { d.dissolveDurationMillis.milliseconds }

    val blurRadius by animateDpAsState(
        targetValue = if (isDissolving) d.maxBlurRadius else 0.dp,
        animationSpec = tween(d.dissolveDurationMillis),
        label = "messageDeletionBlur"
    )

    LaunchedEffect(isDissolving) {
        if (isDissolving) {
            delay(duration)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = !isDissolving,
        modifier = modifier.blur(
            radius = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        ),
        exit = fadeOut(
            tween(
                delayMillis = d.dissolveDurationMillis,
                easing = FastOutLinearInEasing
            )
        ) +
            scaleOut(
                targetScale = d.endScale,
                animationSpec = tween(d.dissolveDurationMillis)
            ) +
            shrinkVertically(
                shrinkTowards = Alignment.CenterVertically,
                animationSpec = tween(d.collapseDurationMillis, d.collapseDelayMillis)
            )
    ) {
        content()
    }
}
