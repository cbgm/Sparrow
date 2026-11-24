package com.cbgm.sparrow.feature.onboarding.presentation.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingButton
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_onboarding_tour_intro_text
import com.cbgm.sparrow.resources.feature_onboarding_tour_intro_title
import com.cbgm.sparrow.resources.feature_onboarding_tour_skip
import com.cbgm.sparrow.resources.feature_onboarding_tour_start
import org.jetbrains.compose.resources.stringResource

/** Offers the tour once the identity exists, so nobody lands in dimmed sample screens without knowing why. */
@Composable
fun TourIntro(onUiEvent: (OnboardingUiEvent) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OnboardingPageLayout(
            graphicHeight = Dimens.OnboardingScreen.tourIntroGraphicHeight,
            isContentCentered = true,
            graphic = { TourIntroGraphic() },
            leading = {
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
            },
            action = {
                OnboardingButton(
                    text = stringResource(Res.string.feature_onboarding_tour_start),
                    onClick = { onUiEvent(OnboardingUiEvent.TourStarted) }
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.feature_onboarding_tour_intro_title),
                style = onboardingTitleStyle(),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(Res.string.feature_onboarding_tour_intro_text),
                style = onboardingBodyStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A small dimmed chat list with one ring lit, the way the tour will look. */
@Composable
private fun TourIntroGraphic() {
    Column(
        modifier =
            Modifier
                .width(Dimens.OnboardingScreen.tourIntroGraphicWidth)
                .height(Dimens.OnboardingScreen.tourIntroGraphicHeight)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().alpha(Alpha.Disabled),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End)
        ) {
            listOf(SparrowIcons.ShieldCheck, SparrowIcons.Search, SparrowIcons.Mail).forEach { icon ->
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(Dimens.OnboardingScreen.tourIntroIconSize))
            }
        }
        TourIntroRow(isLit = true)
        TourIntroRow(isLit = false, modifier = Modifier.alpha(Alpha.Disabled))
        TourIntroRow(isLit = false, modifier = Modifier.alpha(Alpha.Disabled))
    }
}

@Composable
private fun TourIntroRow(
    isLit: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SparrowAvatar(
            name = "",
            target = null,
            size = Dimens.OnboardingScreen.tourIntroAvatarSize,
            ring = if (isLit) AvatarRing.PENDING else AvatarRing.NONE,
            modifier =
                if (isLit) {
                    Modifier
                        .border(Dimens.OnboardingScreen.spotlightLineWidth, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(MaterialTheme.spacing.micro)
                } else {
                    Modifier
                }
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
            TourIntroLine(width = Dimens.OnboardingScreen.tourIntroNameWidth, isName = true)
            TourIntroLine(width = Dimens.OnboardingScreen.tourIntroPreviewWidth, isName = false)
        }
    }
}

@Composable
private fun TourIntroLine(
    width: Dp,
    isName: Boolean
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(Dimens.OnboardingScreen.tourIntroLineHeight)
                .background(
                    if (isName) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.shapes.small
                )
    )
}

@Preview
@Composable
private fun TourIntroPreview() {
    SparrowTheme {
        TourIntro(onUiEvent = {})
    }
}
