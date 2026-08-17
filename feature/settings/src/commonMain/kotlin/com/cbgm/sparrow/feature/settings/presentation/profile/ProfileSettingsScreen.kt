package com.cbgm.sparrow.feature.settings.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.profile.platform.ProfilePictureImage
import com.cbgm.sparrow.feature.settings.presentation.profile.platform.rememberProfilePictureEditorLauncher
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_profile
import com.cbgm.sparrow.resources.feature_settings_profile_picture_add
import com.cbgm.sparrow.resources.feature_settings_profile_picture_change
import com.cbgm.sparrow.resources.feature_settings_profile_picture_description
import com.cbgm.sparrow.resources.feature_settings_profile_picture_remove
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    uiState: ProfileSettingsUiState,
    onUiEvent: (ProfileSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val launchPictureEditor = rememberProfilePictureEditorLauncher { bytes ->
        onUiEvent(ProfileSettingsUiEvent.PictureSelected(bytes))
    }

    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(onBack = { onUiEvent(ProfileSettingsUiEvent.BackClicked) })
        }
    ) { innerPadding, _ ->
        Content(
            uiState = uiState,
            innerPadding = innerPadding,
            onChangePicture = launchPictureEditor,
            onRemovePicture = { onUiEvent(ProfileSettingsUiEvent.RemovePictureClicked) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_profile),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
    )
}

@Composable
private fun Content(
    uiState: ProfileSettingsUiState,
    innerPadding: PaddingValues,
    onChangePicture: () -> Unit,
    onRemovePicture: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.size(MaterialTheme.spacing.large))

        ProfilePicture(
            picture = uiState.profilePicture,
            isSaving = uiState.isSaving
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.large))

        Text(
            text = stringResource(Res.string.feature_settings_profile_picture_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.medium))

        SparrowSecondaryButton(
            onClick = onChangePicture,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            content = {
                Text(
                    text = stringResource(
                        if (uiState.profilePicture.hasPicture) {
                            Res.string.feature_settings_profile_picture_change
                        } else {
                            Res.string.feature_settings_profile_picture_add
                        }
                    )
                )
            }
        )

        if (uiState.profilePicture.hasPicture) {
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

            SparrowOutlinedButton(
                onClick = onRemovePicture,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(text = stringResource(Res.string.feature_settings_profile_picture_remove))
                }
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProfilePicture(
    picture: LocalProfilePicture,
    isSaving: Boolean
) {
    Box(
        modifier =
            Modifier
                .size(156.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bytes = picture.bytes
        if (bytes != null) {
            ProfilePictureImage(
                bytes = bytes,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(72.dp)
            )
        }

        if (isSaving) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Preview
@Composable
fun ProfileSettingsScreenPreview() {
    SparrowTheme {
        ProfileSettingsScreen(
            uiState = ProfileSettingsUiState(
                profilePicture = LocalProfilePicture(),
                isSaving = true,
                errorMessage = "fsfdsf"
            ),
            onUiEvent = {}
        )
    }
}
