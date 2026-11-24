package com.cbgm.sparrow.feature.onboarding.presentation.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.TourStep
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingButton
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_tour_done
import com.cbgm.sparrow.resources.feature_onboarding_tour_dots_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_dots_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_hold_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_hold_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_mailbox_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_mailbox_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_next
import com.cbgm.sparrow.resources.feature_onboarding_tour_ring_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_ring_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_search_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_search_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_shield_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_shield_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_skip
import com.cbgm.sparrow.resources.feature_onboarding_tour_swipe_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_swipe_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The key features no screen explains by itself, shown once after the identity is created. A sample chat list and a
 * sample chat stand in for the still empty app; the explained element stays lit, everything else dims.
 */
@Composable
fun KeyFeatureTour(
    step: TourStep,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    val spots = remember { mutableStateMapOf<TourStep, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spotOf: (TourStep) -> Modifier = { target ->
        Modifier.onGloballyPositioned { coordinates -> spots[target] = coordinates.boundsInRoot() }
    }
    val swipeDistance = with(LocalDensity.current) { Dimens.OnboardingScreen.tourSwipeDistance.toPx() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates -> origin = coordinates.positionInRoot() }
                .tourSwipes(step = step, distance = swipeDistance, onUiEvent = onUiEvent)
    ) {
        when (step) {
            TourStep.RING, TourStep.SHIELD, TourStep.SEARCH, TourStep.MAILBOX, TourStep.SWIPE ->
                SampleChats(
                    spotOf = spotOf,
                    onDelete = { onUiEvent(OnboardingUiEvent.TourNextClicked) }
                )

            TourStep.DOTS, TourStep.HOLD ->
                SampleChat(spotOf = spotOf)
        }
        spots[step]?.translate(-origin)?.let { spot -> Spotlight(spot = spot) }
        TourText(step = step, onUiEvent = onUiEvent)
    }
}

/**
 * Swiping left goes on (on the last step it ends the tour), swiping right goes back, like the onboarding pages. A swipe
 * the sample row takes for itself (on the swipe step) does not turn the page.
 */
private fun Modifier.tourSwipes(
    step: TourStep,
    distance: Float,
    onUiEvent: (OnboardingUiEvent) -> Unit
): Modifier =
    pointerInput(step) {
        var dragged = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = {
                when {
                    dragged < -distance -> onUiEvent(OnboardingUiEvent.TourNextClicked)
                    dragged > distance -> onUiEvent(OnboardingUiEvent.TourBackSwiped)
                }
            }
        ) { _, amount -> dragged += amount }
    }

/** Dims everything but [spot]; it lets touches through, so the lit row can really be swiped. */
@Composable
private fun Spotlight(spot: Rect) {
    val veil = MaterialTheme.colorScheme.background.copy(alpha = Alpha.Tour.dim)
    val line = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val padding = Dimens.OnboardingScreen.spotlightPadding.toPx()
        val hole = spot.inflate(padding)
        val corner = CornerRadius(Dimens.OnboardingScreen.spotlightCorner.toPx())
        drawRect(veil)
        drawRoundRect(Color.Transparent, hole.topLeft, hole.size, corner, blendMode = BlendMode.Clear)
        drawRoundRect(line, hole.topLeft, hole.size, corner, style = Stroke(Dimens.OnboardingScreen.spotlightLineWidth.toPx()))
    }
}

/** The same place on every step, below the sample screens: the text at a fixed height, the actions in the bottom bar. */
@Composable
private fun TourText(
    step: TourStep,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    val isLast = step == TourStep.entries.last()
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Dimens.OnboardingScreen.horizontalPadding)) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.OnboardingScreen.tourTextTop)) {
            Text(text = stringResource(step.title), style = onboardingTitleStyle(), color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(text = stringResource(step.text), style = onboardingBodyStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.OnboardingScreen.bottomPadding)
                    .height(Dimens.OnboardingScreen.bottomBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${step.ordinal + 1}  /  ${TourStep.entries.size}",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = STEP_SPACING.sp),
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large), verticalAlignment = Alignment.CenterVertically) {
                if (step == TourStep.entries.first()) {
                    Text(
                        text = stringResource(Res.string.feature_onboarding_tour_skip),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                        color = MaterialTheme.colorScheme.outline,
                        modifier =
                            Modifier
                                .heightIn(min = Dimens.Button.minTouchHeight)
                                .clickable(role = Role.Button) { onUiEvent(OnboardingUiEvent.TourSkipped) }
                                .padding(vertical = MaterialTheme.spacing.small)
                    )
                }
                OnboardingButton(
                    text = stringResource(if (isLast) Res.string.feature_onboarding_tour_done else Res.string.feature_onboarding_tour_next),
                    onClick = { onUiEvent(OnboardingUiEvent.TourNextClicked) }
                )
            }
        }
    }
}

private val TourStep.title: StringResource
    get() =
        when (this) {
            TourStep.RING -> Res.string.feature_onboarding_tour_ring_title
            TourStep.SHIELD -> Res.string.feature_onboarding_tour_shield_title
            TourStep.SEARCH -> Res.string.feature_onboarding_tour_search_title
            TourStep.MAILBOX -> Res.string.feature_onboarding_tour_mailbox_title
            TourStep.DOTS -> Res.string.feature_onboarding_tour_dots_title
            TourStep.HOLD -> Res.string.feature_onboarding_tour_hold_title
            TourStep.SWIPE -> Res.string.feature_onboarding_tour_swipe_title
        }

private val TourStep.text: StringResource
    get() =
        when (this) {
            TourStep.RING -> Res.string.feature_onboarding_tour_ring_text
            TourStep.SHIELD -> Res.string.feature_onboarding_tour_shield_text
            TourStep.SEARCH -> Res.string.feature_onboarding_tour_search_text
            TourStep.MAILBOX -> Res.string.feature_onboarding_tour_mailbox_text
            TourStep.DOTS -> Res.string.feature_onboarding_tour_dots_text
            TourStep.HOLD -> Res.string.feature_onboarding_tour_hold_text
            TourStep.SWIPE -> Res.string.feature_onboarding_tour_swipe_text
        }

@Preview
@Composable
private fun KeyFeatureTourPreview() {
    SparrowTheme {
        KeyFeatureTour(step = TourStep.RING, onUiEvent = {})
    }
}

private const val STEP_SPACING = 1.5f
