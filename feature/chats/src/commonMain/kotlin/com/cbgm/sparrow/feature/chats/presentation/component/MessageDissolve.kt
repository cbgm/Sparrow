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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val DISSOLVE_DURATION_MILLIS = 350
private const val COLLAPSE_DURATION_MILLIS = 250
private const val COLLAPSE_DELAY_MILLIS = 100
private const val DISSOLVE_END_SCALE = 0.92f

internal class DissolvingMessageListState<T>(
    initialMessages: List<T>,
    private val idOf: (T) -> String,
    private val shouldDissolve: (T) -> Boolean
) {
    private val visibleMessages =
        mutableStateListOf<T>().apply {
            addAll(initialMessages)
        }

    private var dissolvingMessageId by mutableStateOf<String?>(null)

    val messages: List<T>
        get() = visibleMessages

    fun update(messages: List<T>) {
        val incomingById = messages.associateBy(idOf)

        visibleMessages.indices.forEach { index ->
            val messageId = idOf(visibleMessages[index])

            incomingById[messageId]?.let { updatedMessage ->
                visibleMessages[index] = updatedMessage

                if (messageId == dissolvingMessageId) {
                    dissolvingMessageId = null
                }
            }
        }

        val missingMessages =
            visibleMessages.filter {
                idOf(it) !in incomingById
            }

        missingMessages.forEach { message ->
            val messageId = idOf(message)

            if (
                dissolvingMessageId == null &&
                shouldDissolve(message)
            ) {
                dissolvingMessageId = messageId
            } else if (messageId != dissolvingMessageId) {
                visibleMessages.remove(message)
            }
        }

        val visibleIds =
            visibleMessages.mapTo(mutableSetOf(), idOf)

        messages
            .filterNot { idOf(it) in visibleIds }
            .asReversed()
            .forEach { message ->
                visibleMessages.add(0, message)
            }
    }

    fun isDissolving(messageId: String): Boolean =
        messageId == dissolvingMessageId

    fun finishDissolve(messageId: String) {
        if (messageId != dissolvingMessageId) return

        visibleMessages.removeAll {
            idOf(it) == messageId
        }

        dissolvingMessageId = null
    }
}

@Composable
internal fun <T> rememberDissolvingMessageListState(
    messages: List<T>,
    idOf: (T) -> String,
    shouldDissolve: (T) -> Boolean
): DissolvingMessageListState<T> {
    val state =
        remember {
            DissolvingMessageListState(
                initialMessages = messages,
                idOf = idOf,
                shouldDissolve = shouldDissolve
            )
        }

    LaunchedEffect(messages) {
        state.update(messages)
    }

    return state
}

@Composable
internal fun <T> MessageDissolve(
    messageId: String,
    state: DissolvingMessageListState<T>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDissolving = state.isDissolving(messageId)

    val blurRadius by
        animateDpAsState(
            targetValue =
                if (isDissolving) {
                    Dimens.MessageDeletion.maxBlurRadius
                } else {
                    0.dp
                },
            animationSpec =
                tween(
                    durationMillis = DISSOLVE_DURATION_MILLIS
                ),
            label = "messageDeletionBlur"
        )

    LaunchedEffect(isDissolving) {
        if (isDissolving) {
            delay(DISSOLVE_DURATION_MILLIS.milliseconds)
            state.finishDissolve(messageId)
        }
    }

    AnimatedVisibility(
        visible = !isDissolving,
        modifier =
            modifier.blur(
                radius = blurRadius,
                edgeTreatment = BlurredEdgeTreatment.Unbounded
            ),
        exit =
            fadeOut(
                animationSpec =
                    tween(
                        durationMillis = DISSOLVE_DURATION_MILLIS,
                        easing = FastOutLinearInEasing
                    )
            ) +
                scaleOut(
                    targetScale = DISSOLVE_END_SCALE,
                    animationSpec =
                        tween(
                            durationMillis = DISSOLVE_DURATION_MILLIS
                        )
                ) +
                shrinkVertically(
                    shrinkTowards = Alignment.CenterVertically,
                    animationSpec =
                        tween(
                            durationMillis = COLLAPSE_DURATION_MILLIS,
                            delayMillis = COLLAPSE_DELAY_MILLIS
                        )
                )
    ) {
        content()
    }
}
