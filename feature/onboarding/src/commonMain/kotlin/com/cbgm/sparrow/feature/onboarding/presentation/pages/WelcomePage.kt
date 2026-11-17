package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cbgm.sparrow.core.ui.component.SparrowAnimation
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_app_name
import com.cbgm.sparrow.resources.feature_onboarding_swipe
import com.cbgm.sparrow.resources.feature_onboarding_welcome_line
import org.jetbrains.compose.resources.stringResource

/** The Sparrow mark, its name and one line; a faint "swipe" shows how to go on. */
@Composable
fun WelcomePage(active: Boolean) {
    val clock = rememberPageClock(active)
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.markGraphicHeight,
        graphic = { SparrowAnimation(modifier = Modifier.size(Dimens.OnboardingScreen.markSize), animated = true) },
        hint = { SwipeHint() }
    ) {
        FadingText(
            text = stringResource(Res.string.base_app_name),
            clock = clock,
            startMillis = TITLE_START_MILLISECONDS,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        FadingText(
            text = stringResource(Res.string.feature_onboarding_welcome_line),
            clock = clock,
            startMillis = LINE_START_MILLISECONDS,
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwipeHint() {
    val beat by rememberInfiniteTransition(label = "swipeHint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(HINT_MILLISECONDS), RepeatMode.Reverse),
        label = "swipeHintBeat"
    )
    Text(
        text = stringResource(Res.string.feature_onboarding_swipe),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light, letterSpacing = HINT_SPACING.sp),
        color = MaterialTheme.colorScheme.outline,
        modifier =
            Modifier.graphicsLayer {
                alpha = HINT_DIM + beat * (1f - HINT_DIM)
                translationX = -beat * HINT_SHIFT.dp.toPx()
            }
    )
}

@Preview
@Composable
private fun WelcomePagePreview() {
    SparrowTheme { WelcomePage(active = true) }
}

private const val TITLE_START_MILLISECONDS = 300L
private const val LINE_START_MILLISECONDS = 800L
private const val HINT_MILLISECONDS = 1_200
private const val HINT_DIM = 0.3f
private const val HINT_SHIFT = 10
private const val HINT_SPACING = 3f
