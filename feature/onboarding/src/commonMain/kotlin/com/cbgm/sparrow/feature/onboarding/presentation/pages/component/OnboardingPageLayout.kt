package com.cbgm.sparrow.feature.onboarding.presentation.pages.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun OnboardingPageLayout(
    graphicHeight: Dp,
    graphic: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    isContentCentered: Boolean = false,
    action: @Composable () -> Unit = {},
    hint: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize().onboardingBars()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = Dimens.OnboardingScreen.bottomBarHeight + Dimens.OnboardingScreen.bottomPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.OnboardingScreen.horizontalPadding)
                    .padding(top = Dimens.OnboardingScreen.topPadding)
        ) {
            if (isContentCentered) {
                Spacer(Modifier.weight(1f))
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(graphicHeight),
                contentAlignment = Alignment.Center,
                content = graphic
            )
            Spacer(Modifier.height(MaterialTheme.spacing.base.times(5)))
            content()

            if (isContentCentered) {
                Spacer(Modifier.weight(1f))
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).bottomBar(), contentAlignment = Alignment.Center) {
            hint()
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).bottomBar(), contentAlignment = Alignment.CenterEnd) {
            action()
        }
    }
}

@Composable
fun PageNumber(
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.onboardingBars().bottomBar(), contentAlignment = Alignment.CenterStart) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(twoDigits(page + 1)) }
                    append("  /  ${twoDigits(pageCount)}")
                },
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = PAGE_NUMBER_SPACING.sp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
@ReadOnlyComposable
fun onboardingTitleStyle(): TextStyle =
    MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light, letterSpacing = TITLE_SPACING.sp)

@Composable
@ReadOnlyComposable
fun onboardingBodyStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light)

private fun Modifier.onboardingBars(): Modifier = systemBarsPadding().imePadding()

private fun Modifier.bottomBar(): Modifier =
    padding(horizontal = Dimens.OnboardingScreen.horizontalPadding)
        .padding(bottom = Dimens.OnboardingScreen.bottomPadding)
        .height(Dimens.OnboardingScreen.bottomBarHeight)

private fun twoDigits(number: Int): String = number.toString().padStart(2, '0')

private const val PAGE_NUMBER_SPACING = 1.5f
private const val TITLE_SPACING = -0.5f
