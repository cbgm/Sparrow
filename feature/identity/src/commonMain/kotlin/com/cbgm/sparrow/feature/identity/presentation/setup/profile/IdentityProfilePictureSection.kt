package com.cbgm.sparrow.feature.identity.presentation.setup.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_profile_picture
import org.jetbrains.compose.resources.stringResource

/** Same avatar + camera affordance used in Group Details, editing the local identity's picture. */
@Composable
fun IdentityProfilePictureSection(
    state: IdentityProfilePictureUiState,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            SparrowAvatar(
                name = "",
                target = AvatarTarget.LocalUser,
                size = Dimens.GroupDetailsScreen.avatarSize,
                modifier = Modifier.padding(MaterialTheme.spacing.base)
            )

            IconButton(
                onClick = onEdit,
                enabled = !state.isSaving,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(Res.string.feature_settings_profile_picture),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.GroupDetailsScreen.avatarProgressSize),
                    strokeWidth = Dimens.GroupDetailsScreen.avatarProgressStrokeWidth
                )
            }
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
