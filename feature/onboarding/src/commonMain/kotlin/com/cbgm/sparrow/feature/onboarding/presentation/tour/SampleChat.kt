package com.cbgm.sparrow.feature.onboarding.presentation.tour

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.DayRailState
import com.cbgm.sparrow.core.ui.component.DeliveryDotsState
import com.cbgm.sparrow.core.ui.component.SparrowDaySeparator
import com.cbgm.sparrow.core.ui.component.SparrowDeliveryDots
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.messageBubble
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.time.MILLISECONDS_PER_DAY
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.core.util.time.localEpochDay
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.onboarding.presentation.model.TourStep
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_delivered
import com.cbgm.sparrow.resources.feature_chats_failed_tap_to_retry
import com.cbgm.sparrow.resources.feature_chats_queued
import com.cbgm.sparrow.resources.feature_chats_read
import com.cbgm.sparrow.resources.feature_chats_sending
import com.cbgm.sparrow.resources.feature_chats_sent
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_answer
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_draft
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_follow_up
import com.cbgm.sparrow.resources.feature_onboarding_tour_sample_question
import org.jetbrains.compose.resources.stringResource

/**
 * A chat like the real one: their question, your answer that was read, and today either a follow-up that arrived or,
 * on the dots step, a draft to send yourself. Holding their question and holding a date work like in the app.
 */
@Composable
internal fun SampleChat(
    step: TourStep,
    tried: TourTryState,
    railState: DayRailState,
    spotOf: (TourStep) -> Modifier,
    onTry: (TourTry) -> Unit
) {
    val now = remember { SystemClock.nowEpochMilliseconds() }
    val today = remember(now) { localEpochDay(now) }
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        SparrowTopBar(
            navigationIcon = {
                Icon(
                    imageVector = SparrowIcons.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
                )
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SparrowAvatar(
                        name = SAMPLE_CONTACT,
                        target = null,
                        size = Dimens.DirectConversationScreen.topBarAvatarSize,
                        ring = AvatarRing.PENDING
                    )
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(text = SAMPLE_CONTACT, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Light)
                }
            },
            actions = {
                Row(
                    modifier = Modifier.padding(end = MaterialTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
                ) {
                    Icon(
                        imageVector = SparrowIcons.ShieldCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.TopBar.iconSize)
                    )
                    Icon(imageVector = SparrowIcons.Search, contentDescription = null, modifier = Modifier.size(Dimens.TopBar.iconSize))
                }
            },
            windowInsets = WindowInsets(0)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            SparrowDaySeparator(
                writtenAt = now - MILLISECONDS_PER_DAY,
                today = today,
                railState = railState,
                onDayPicked = { onTry(TourTry.DayPicked) }
            )
            Column(horizontalAlignment = Alignment.Start) {
                SampleBubble(
                    text = stringResource(Res.string.feature_onboarding_tour_sample_question),
                    time = "09:14",
                    isMine = false,
                    modifier =
                        spotOf(TourStep.HOLD).pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onTry(TourTry.MessageHeld) })
                        }
                )
                tried.reaction?.let { emoji -> SampleReaction(emoji = emoji) }
            }
            SampleBubble(
                text = stringResource(Res.string.feature_onboarding_tour_sample_answer),
                time = "09:15",
                isMine = true
            ) {
                SparrowDeliveryDots(state = DeliveryDotsState.READ, description = stringResource(Res.string.feature_chats_read))
            }
            SparrowDaySeparator(
                writtenAt = now,
                today = today,
                railState = railState,
                onDayPicked = { onTry(TourTry.DayPicked) },
                modifier = spotOf(TourStep.DAYS)
            )
            when {
                step != TourStep.DOTS ->
                    SampleBubble(
                        text = stringResource(Res.string.feature_onboarding_tour_sample_follow_up),
                        time = "09:16",
                        isMine = true
                    ) {
                        SparrowDeliveryDots(
                            state = DeliveryDotsState.DELIVERED,
                            description = stringResource(Res.string.feature_chats_delivered)
                        )
                    }

                tried.sentDots == null ->
                    SampleDraft(
                        text = stringResource(Res.string.feature_onboarding_tour_sample_draft),
                        onSend = { onTry(TourTry.DraftSent) },
                        modifier = spotOf(TourStep.DOTS)
                    )

                else ->
                    SampleBubble(
                        text = stringResource(Res.string.feature_onboarding_tour_sample_draft),
                        time = "09:17",
                        isMine = true,
                        modifier = spotOf(TourStep.DOTS)
                    ) {
                        SparrowDeliveryDots(state = tried.sentDots, description = deliveryDescription(tried.sentDots))
                    }
            }
        }
    }
}

/** The message field with a message ready to send. */
@Composable
private fun SampleDraft(
    text: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(start = MaterialTheme.spacing.medium, end = MaterialTheme.spacing.micro),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onSend) {
                Icon(imageVector = SparrowIcons.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** A reaction under a bubble, like the chat shows it. */
@Composable
private fun SampleReaction(emoji: String) {
    Surface(
        modifier = Modifier.padding(top = MaterialTheme.spacing.micro),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.micro),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun deliveryDescription(dots: DeliveryDotsState): String =
    stringResource(
        when (dots) {
            DeliveryDotsState.QUEUED -> Res.string.feature_chats_queued
            DeliveryDotsState.SENDING -> Res.string.feature_chats_sending
            DeliveryDotsState.SENT -> Res.string.feature_chats_sent
            DeliveryDotsState.DELIVERED -> Res.string.feature_chats_delivered
            DeliveryDotsState.READ -> Res.string.feature_chats_read
            DeliveryDotsState.FAILED -> Res.string.feature_chats_failed_tap_to_retry
        }
    )

@Composable
private fun SampleBubble(
    text: String,
    time: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    delivery: (@Composable () -> Unit)? = null
) {
    val shapes = MaterialTheme.shapes.messageBubble
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = modifier.widthIn(max = Dimens.OnboardingScreen.sampleBubbleMaxWidth),
            color =
                if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = Alpha.MessageBubble.mine)
                        .compositeOver(MaterialTheme.colorScheme.background)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = if (isMine) shapes.mine else shapes.theirs
        ) {
            // Like a real bubble: the time and dots at the end of the (short) text, inside the bubble.
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.base),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
                ) {
                    Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    delivery?.invoke()
                }
            }
        }
    }
}
