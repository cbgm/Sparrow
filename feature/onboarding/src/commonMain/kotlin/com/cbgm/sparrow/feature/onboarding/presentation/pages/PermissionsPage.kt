package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingAction
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic.PermissionDotsGraphic
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_contacts
import com.cbgm.sparrow.resources.base_notifications
import com.cbgm.sparrow.resources.base_phone_number
import com.cbgm.sparrow.resources.feature_onboarding_allow
import com.cbgm.sparrow.resources.feature_onboarding_audio
import com.cbgm.sparrow.resources.feature_onboarding_audio_description
import com.cbgm.sparrow.resources.feature_onboarding_camera
import com.cbgm.sparrow.resources.feature_onboarding_camera_description
import com.cbgm.sparrow.resources.feature_onboarding_contacts_permission_description
import com.cbgm.sparrow.resources.feature_onboarding_notifications_description
import com.cbgm.sparrow.resources.feature_onboarding_permissions_only_what_you_use
import com.cbgm.sparrow.resources.feature_onboarding_permissions_settings_hint
import com.cbgm.sparrow.resources.feature_onboarding_permissions_title
import com.cbgm.sparrow.resources.feature_onboarding_phone_number_permission_description
import com.cbgm.sparrow.resources.feature_onboarding_storage
import com.cbgm.sparrow.resources.feature_onboarding_storage_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The permissions Sparrow asks for, sliding in one by one with what each is for; Allow (or swiping on) asks for them. */
@Composable
fun PermissionsPage(
    active: Boolean,
    onAllow: () -> Unit
) {
    val clock = rememberPageClock(active)
    val afterList = FIRST_ROW_MILLISECONDS + PERMISSIONS.size * ROW_STEP_MILLISECONDS
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.dotsGraphicHeight,
        graphic = {
            PermissionDotsGraphic(
                count = PERMISSIONS.size,
                clock = clock,
                firstMillis = FIRST_ROW_MILLISECONDS,
                stepMillis = ROW_STEP_MILLISECONDS,
                modifier = Modifier.fillMaxSize()
            )
        },
        action = {
            OnboardingAction(
                text = stringResource(Res.string.feature_onboarding_allow),
                clock = clock,
                startMillis = afterList,
                onClick = onAllow
            )
        }
    ) {
        FadingText(
            text = stringResource(Res.string.feature_onboarding_permissions_title),
            clock = clock,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        FadingText(
            text = stringResource(Res.string.feature_onboarding_permissions_only_what_you_use),
            clock = clock,
            startMillis = SUBTITLE_START_MILLISECONDS,
            style = onboardingBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        PERMISSIONS.forEachIndexed { index, (name, description) ->
            PermissionRow(
                name = stringResource(name),
                description = stringResource(description),
                clock = clock,
                startMillis = FIRST_ROW_MILLISECONDS + index * ROW_STEP_MILLISECONDS,
                divided = index > 0
            )
        }
        Text(
            text = stringResource(Res.string.feature_onboarding_permissions_settings_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier =
                Modifier
                    .padding(top = MaterialTheme.spacing.medium)
                    .graphicsLayer { alpha = clock.progress(afterList, ROW_MILLISECONDS) }
        )
    }
}

@Composable
private fun PermissionRow(
    name: String,
    description: String,
    clock: State<Long>,
    startMillis: Long,
    divided: Boolean
) {
    val slide = Dimens.OnboardingScreen.rowSlide
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val shown = clock.progress(startMillis, ROW_MILLISECONDS)
                    alpha = shown
                    translationX = (1f - shown) * slide.toPx()
                }
    ) {
        if (divided) HorizontalDivider(thickness = Dimens.OnboardingScreen.lineWidth, color = MaterialTheme.colorScheme.divider)
        Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.base)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** In the order the dots light up. */
private val PERMISSIONS: List<Pair<StringResource, StringResource>> =
    listOf(
        Res.string.base_notifications to Res.string.feature_onboarding_notifications_description,
        Res.string.base_contacts to Res.string.feature_onboarding_contacts_permission_description,
        Res.string.feature_onboarding_camera to Res.string.feature_onboarding_camera_description,
        Res.string.feature_onboarding_audio to Res.string.feature_onboarding_audio_description,
        Res.string.feature_onboarding_storage to Res.string.feature_onboarding_storage_description,
        Res.string.base_phone_number to Res.string.feature_onboarding_phone_number_permission_description
    )

@Preview
@Composable
private fun PermissionsPagePreview() {
    SparrowTheme { PermissionsPage(active = true, onAllow = {}) }
}

private const val SUBTITLE_START_MILLISECONDS = 400L
private const val FIRST_ROW_MILLISECONDS = 900L
private const val ROW_STEP_MILLISECONDS = 380L
private const val ROW_MILLISECONDS = 700L
