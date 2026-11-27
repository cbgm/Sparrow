package com.cbgm.sparrow.feature.onboarding.presentation.tour

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.cbgm.sparrow.core.ui.component.DayRailState
import com.cbgm.sparrow.core.ui.component.DeliveryDotsState
import com.cbgm.sparrow.core.ui.component.SparrowDayRail
import com.cbgm.sparrow.core.ui.component.SparrowMessageActionMenu
import com.cbgm.sparrow.core.ui.component.sampleDayRailDays
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.core.util.time.localEpochDay
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.TourStep
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingButton
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_tour_days_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_days_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_done
import com.cbgm.sparrow.resources.feature_onboarding_tour_dots_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_dots_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_hint_days
import com.cbgm.sparrow.resources.feature_onboarding_tour_hint_dots
import com.cbgm.sparrow.resources.feature_onboarding_tour_hint_hold
import com.cbgm.sparrow.resources.feature_onboarding_tour_hint_swipe
import com.cbgm.sparrow.resources.feature_onboarding_tour_hint_top_bar
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
import com.cbgm.sparrow.resources.feature_onboarding_tour_swipe_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_swipe_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_top_bar_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
    var tried by remember(step) { mutableStateOf(TourTryState()) }
    val onTry: (TourTry) -> Unit = { attempt -> tried = tried.after(attempt) }
    val railState = remember(step) { DayRailState() }
    TourTryEffects(tried = tried, onTry = onTry, onUiEvent = onUiEvent)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates -> origin = coordinates.positionInRoot() }
                .tourSwipes(step = step, distance = swipeDistance, onUiEvent = onUiEvent)
    ) {
        when (step) {
            TourStep.RING, TourStep.TOP_BAR, TourStep.SWIPE ->
                SampleChats(
                    spotOf = spotOf,
                    onIconTapped = { icon -> onTry(TourTry.IconTapped(icon)) },
                    onDelete = { onUiEvent(OnboardingUiEvent.TourNextClicked) }
                )

            TourStep.DOTS, TourStep.HOLD, TourStep.DAYS ->
                SampleChat(step = step, tried = tried, railState = railState, spotOf = spotOf, onTry = onTry)
        }
        spots[step]?.translate(-origin)?.let { spot -> Spotlight(spot = spot) }
        TourText(
            step = step,
            tried = tried,
            showsText = !tried.isMenuOpen && !railState.isOpen,
            onUiEvent = onUiEvent
        )
        spots[TourStep.HOLD]
            ?.takeIf { step == TourStep.HOLD && tried.isMenuOpen }
            ?.translate(-origin)
            ?.let { spot -> SampleMessageMenu(under = spot, onTry = onTry) }
        if (step == TourStep.DAYS) {
            SparrowDayRail(
                state = railState,
                days = remember { sampleDayRailDays(SystemClock.nowEpochMilliseconds()) },
                today = remember { localEpochDay(SystemClock.nowEpochMilliseconds()) },
                contentPadding = WindowInsets.systemBars.asPaddingValues(),
                onDayPicked = { onTry(TourTry.DayPicked) }
            )
        }
    }
}

/** Plays what was tried: the sent message's dots fill one after another, and a finished step moves on. */
@Composable
private fun TourTryEffects(
    tried: TourTryState,
    onTry: (TourTry) -> Unit,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    val isSent = tried.sentDots != null
    LaunchedEffect(isSent) {
        if (!isSent) return@LaunchedEffect
        listOf(DeliveryDotsState.SENT, DeliveryDotsState.DELIVERED, DeliveryDotsState.READ).forEach { dots ->
            delay(DOTS_STEP_MILLISECONDS.milliseconds)
            onTry(TourTry.DotsAdvanced(dots))
        }
    }
    LaunchedEffect(tried.isDone) {
        if (!tried.isDone) return@LaunchedEffect
        delay(ADVANCE_DELAY_MILLISECONDS.milliseconds)
        onUiEvent(OnboardingUiEvent.TourNextClicked)
    }
}

/** The reactions and actions the chat shows for a held message, right under it; a reaction ends the step. */
@Composable
private fun SampleMessageMenu(
    under: Rect,
    onTry: (TourTry) -> Unit
) {
    val gap = with(LocalDensity.current) { MaterialTheme.spacing.base.toPx() }
    val dismiss = { onTry(TourTry.MenuDismissed) }
    Box(modifier = Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = dismiss)) {
        Box(modifier = Modifier.offset { IntOffset(under.left.roundToInt(), (under.bottom + gap).roundToInt()) }) {
            SparrowMessageActionMenu(
                color = MaterialTheme.colorScheme.surfaceContainer,
                onReplyClick = dismiss,
                onForwardClick = dismiss,
                onReactionClick = { emoji -> onTry(TourTry.Reacted(emoji)) },
                showEdit = false,
                onEditClick = dismiss,
                onCopyClick = dismiss,
                showDelete = false,
                onDeleteClick = dismiss
            )
        }
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
    tried: TourTryState,
    showsText: Boolean,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    val isLast = step == TourStep.entries.last()
    val textAlpha by animateFloatAsState(if (showsText) Alpha.Opaque else Alpha.Invisible)
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Dimens.OnboardingScreen.horizontalPadding)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.OnboardingScreen.tourTextTop)
                    .graphicsLayer { alpha = textAlpha }
        ) {
            Text(text = stringResource(step.title), style = onboardingTitleStyle(), color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            TourBody(step = step, tried = tried)
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
                step.hint?.let { hint ->
                    Text(
                        text = stringResource(hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light),
                        color = MaterialTheme.colorScheme.outline
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

@Composable
private fun TourBody(
    step: TourStep,
    tried: TourTryState
) {
    when (step) {
        TourStep.TOP_BAR ->
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                val lit = tried.tappedIcons.lastOrNull()
                TopBarIconLine(
                    icon = SparrowIcons.ShieldCheck,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    name = Res.string.feature_onboarding_tour_shield_title,
                    text = Res.string.feature_onboarding_tour_shield_text,
                    lit = lit?.let { it == TopBarIcon.SHIELD }
                )
                TopBarIconLine(
                    icon = SparrowIcons.Search,
                    tint = MaterialTheme.colorScheme.onBackground,
                    name = Res.string.feature_onboarding_tour_search_title,
                    text = Res.string.feature_onboarding_tour_search_text,
                    lit = lit?.let { it == TopBarIcon.SEARCH }
                )
                TopBarIconLine(
                    icon = SparrowIcons.Mail,
                    tint = MaterialTheme.colorScheme.onBackground,
                    name = Res.string.feature_onboarding_tour_mailbox_title,
                    text = Res.string.feature_onboarding_tour_mailbox_text,
                    lit = lit?.let { it == TopBarIcon.MAILBOX }
                )
            }

        TourStep.RING -> TourBodyText(Res.string.feature_onboarding_tour_ring_text)
        TourStep.DOTS -> TourBodyText(Res.string.feature_onboarding_tour_dots_text)
        TourStep.HOLD -> TourBodyText(Res.string.feature_onboarding_tour_hold_text)
        TourStep.DAYS -> TourBodyText(Res.string.feature_onboarding_tour_days_text)
        TourStep.SWIPE -> TourBodyText(Res.string.feature_onboarding_tour_swipe_text)
    }
}

@Composable
private fun TourBodyText(text: StringResource) {
    Text(text = stringResource(text), style = onboardingBodyStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One icon of the top bar: the icon, its name, then what it does. Once an icon was tapped, only its line stays [lit]. */
@Composable
private fun TopBarIconLine(
    icon: ImageVector,
    tint: Color,
    name: StringResource,
    text: StringResource,
    lit: Boolean?
) {
    val alpha by animateFloatAsState(if (lit == false) Alpha.Disabled else Alpha.Opaque)
    Row(modifier = Modifier.graphicsLayer { this.alpha = alpha }, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (lit == true) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(Dimens.TopBar.iconSize)
        )
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(stringResource(name)) }
                    append("  ·  ")
                    append(stringResource(text))
                },
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** What to try on the step, next to Next; the ring step is only read. */
private val TourStep.hint: StringResource?
    get() =
        when (this) {
            TourStep.RING -> null
            TourStep.TOP_BAR -> Res.string.feature_onboarding_tour_hint_top_bar
            TourStep.DOTS -> Res.string.feature_onboarding_tour_hint_dots
            TourStep.HOLD -> Res.string.feature_onboarding_tour_hint_hold
            TourStep.DAYS -> Res.string.feature_onboarding_tour_hint_days
            TourStep.SWIPE -> Res.string.feature_onboarding_tour_hint_swipe
        }

private val TourStep.title: StringResource
    get() =
        when (this) {
            TourStep.RING -> Res.string.feature_onboarding_tour_ring_title
            TourStep.TOP_BAR -> Res.string.feature_onboarding_tour_top_bar_title
            TourStep.DOTS -> Res.string.feature_onboarding_tour_dots_title
            TourStep.HOLD -> Res.string.feature_onboarding_tour_hold_title
            TourStep.DAYS -> Res.string.feature_onboarding_tour_days_title
            TourStep.SWIPE -> Res.string.feature_onboarding_tour_swipe_title
        }

/** Click or swipe through every step, like after creating an identity (use the interactive preview). */
@Preview
@Composable
private fun KeyFeatureTourPreview() {
    SparrowTheme {
        var step by remember { mutableStateOf(TourStep.entries.first()) }
        KeyFeatureTour(step = step) { event ->
            step =
                when (event) {
                    OnboardingUiEvent.TourNextClicked -> TourStep.entries.getOrElse(step.ordinal + 1) { TourStep.entries.first() }
                    OnboardingUiEvent.TourBackSwiped -> TourStep.entries.getOrElse(step.ordinal - 1) { step }
                    else -> TourStep.entries.first()
                }
        }
    }
}

@Preview
@Composable
private fun KeyFeatureTourRingPreview() {
    TourStepPreview(TourStep.RING)
}

@Preview
@Composable
private fun KeyFeatureTourTopBarPreview() {
    TourStepPreview(TourStep.TOP_BAR)
}

@Preview
@Composable
private fun KeyFeatureTourDotsPreview() {
    TourStepPreview(TourStep.DOTS)
}

@Preview
@Composable
private fun KeyFeatureTourHoldPreview() {
    TourStepPreview(TourStep.HOLD)
}

@Preview
@Composable
private fun KeyFeatureTourDaysPreview() {
    TourStepPreview(TourStep.DAYS)
}

@Preview
@Composable
private fun KeyFeatureTourSwipePreview() {
    TourStepPreview(TourStep.SWIPE)
}

@Composable
private fun TourStepPreview(step: TourStep) {
    SparrowTheme {
        KeyFeatureTour(step = step, onUiEvent = {})
    }
}

private const val STEP_SPACING = 1.5f
private const val DOTS_STEP_MILLISECONDS = 900L
private const val ADVANCE_DELAY_MILLISECONDS = 1_200L
