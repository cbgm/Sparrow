package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowLink
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.FadingText
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingAction
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.OnboardingPageLayout
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingBodyStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.onboardingTitleStyle
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.progress
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.rememberPageClock
import com.cbgm.sparrow.feature.onboarding.presentation.pages.graphic.ContactsGraphic
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_choose_another_number
import com.cbgm.sparrow.resources.base_choose_phone_number
import com.cbgm.sparrow.resources.base_generating_secure_identity
import com.cbgm.sparrow.resources.base_identity_ready_opening
import com.cbgm.sparrow.resources.base_phone_number
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_something_went_wrong
import com.cbgm.sparrow.resources.feature_onboarding_create
import com.cbgm.sparrow.resources.feature_onboarding_detected_automatically_confirm
import com.cbgm.sparrow.resources.feature_onboarding_local_identity_incomplete
import com.cbgm.sparrow.resources.feature_onboarding_no_automatic_number
import com.cbgm.sparrow.resources.feature_onboarding_phone_description
import com.cbgm.sparrow.resources.feature_onboarding_phone_title
import com.cbgm.sparrow.resources.feature_onboarding_preparing_phone_setup
import com.cbgm.sparrow.resources.feature_onboarding_try_sim_number_again
import com.cbgm.sparrow.resources.feature_onboarding_your_name
import org.jetbrains.compose.resources.stringResource

/** "Last thing.": name and number, so people you know can find you; Create makes the identity. */
@Composable
fun PhonePage(
    identityState: IdentityCreateUiState,
    active: Boolean,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit,
    onRestoreIdentity: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    val clock = rememberPageClock(active)
    val form = identityState as? IdentityCreateUiState.Form
    val complete = form != null && form.phoneNumber.isNotBlank() && form.name.isNotBlank()
    OnboardingPageLayout(
        graphicHeight = Dimens.OnboardingScreen.contactsGraphicHeight,
        graphic = { ContactsGraphic(clock = clock, modifier = Modifier.fillMaxSize()) },
        action = {
            if (form != null) {
                OnboardingAction(
                    text = stringResource(Res.string.feature_onboarding_create),
                    clock = clock,
                    startMillis = ACTION_START_MILLISECONDS,
                    onClick = onApproveAndCreate,
                    enabled = complete && !isCreating
                )
            }
        }
    ) {
        FadingText(
            text = stringResource(Res.string.feature_onboarding_phone_title),
            clock = clock,
            style = onboardingTitleStyle(),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        when (identityState) {
            is IdentityCreateUiState.Form ->
                Form(
                    form = identityState,
                    clock = clock,
                    complete = complete,
                    canRetryAutomatic = canRetryAutomatic,
                    onChooseAnotherNumber = onChooseAnotherNumber,
                    onRetryAutomaticNumber = onRetryAutomaticNumber,
                    onPhoneNumberChanged = onPhoneNumberChanged,
                    onRestoreIdentity = onRestoreIdentity,
                    onNameChanged = onNameChanged
                )

            IdentityCreateUiState.Loading -> Working(text = stringResource(Res.string.feature_onboarding_preparing_phone_setup), clock = clock)
            IdentityCreateUiState.Creating -> Working(text = stringResource(Res.string.base_generating_secure_identity), clock = clock)
            IdentityCreateUiState.Completed -> Working(text = stringResource(Res.string.base_identity_ready_opening), clock = clock)
            IdentityCreateUiState.IncompleteIdentity -> Problem(text = stringResource(Res.string.feature_onboarding_local_identity_incomplete))
            IdentityCreateUiState.Error -> Problem(text = stringResource(Res.string.feature_identity_something_went_wrong))
        }
    }
}

@Composable
private fun Form(
    form: IdentityCreateUiState.Form,
    clock: State<Long>,
    complete: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onRestoreIdentity: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    FadingText(
        text = stringResource(Res.string.feature_onboarding_phone_description),
        clock = clock,
        startMillis = DESCRIPTION_START_MILLISECONDS,
        style = onboardingBodyStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(
        modifier =
            Modifier
                .padding(top = MaterialTheme.spacing.large)
                .graphicsLayer { alpha = clock.progress(FIELDS_START_MILLISECONDS, FIELDS_MILLISECONDS) },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        SparrowInputField(
            value = form.name,
            onValueChange = onNameChanged,
            label = stringResource(Res.string.feature_onboarding_your_name),
            isSingleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        SparrowInputField(
            value = form.phoneNumber,
            onValueChange = onPhoneNumberChanged,
            label = stringResource(Res.string.base_phone_number),
            supportingText =
                stringResource(
                    if (form.phoneNumber.isBlank()) Res.string.feature_onboarding_no_automatic_number else Res.string.feature_onboarding_detected_automatically_confirm
                ),
            isSingleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Links(
            phoneNumberBlank = form.phoneNumber.isBlank(),
            canRetryAutomatic = canRetryAutomatic,
            canRestore = complete,
            onChooseAnotherNumber = onChooseAnotherNumber,
            onRetryAutomaticNumber = onRetryAutomaticNumber,
            onRestoreIdentity = onRestoreIdentity
        )
    }
}

/** The less common ways on, as quiet text links. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Links(
    phoneNumberBlank: Boolean,
    canRetryAutomatic: Boolean,
    canRestore: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onRestoreIdentity: () -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
        SparrowLink(
            text = stringResource(if (phoneNumberBlank) Res.string.base_choose_phone_number else Res.string.base_choose_another_number),
            onClick = onChooseAnotherNumber
        )
        if (canRetryAutomatic) SparrowLink(text = stringResource(Res.string.feature_onboarding_try_sim_number_again), onClick = onRetryAutomaticNumber)
        if (canRestore) SparrowLink(text = stringResource(Res.string.feature_identity_backup_restore_action), onClick = onRestoreIdentity)
    }
}

@Composable
private fun Working(
    text: String,
    clock: State<Long>
) {
    FadingText(
        text = text,
        clock = clock,
        startMillis = DESCRIPTION_START_MILLISECONDS,
        style = onboardingBodyStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LinearProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(top = MaterialTheme.spacing.large)
    )
}

@Composable
private fun Problem(text: String) {
    Text(text = text, style = onboardingBodyStyle(), color = MaterialTheme.colorScheme.error)
}

@Preview
@Composable
private fun PhonePagePreview() {
    SparrowTheme {
        PhonePage(
            identityState = IdentityCreateUiState.Form(phoneNumber = "+491512345678", name = "Chris"),
            active = true,
            isCreating = false,
            canRetryAutomatic = true,
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onRestoreIdentity = {},
            onNameChanged = {}
        )
    }
}

private const val DESCRIPTION_START_MILLISECONDS = 400L
private const val FIELDS_START_MILLISECONDS = 1_100L
private const val FIELDS_MILLISECONDS = 800L
private const val ACTION_START_MILLISECONDS = 1_600L
