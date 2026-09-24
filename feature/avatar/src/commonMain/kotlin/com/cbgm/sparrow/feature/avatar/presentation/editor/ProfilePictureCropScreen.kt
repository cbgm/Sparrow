package com.cbgm.sparrow.feature.avatar.presentation.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowRoundApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion
import com.cbgm.sparrow.feature.avatar.presentation.editor.crop.ProfilePictureCropCanvas

@Composable
internal fun ProfilePictureCropScreen(
    image: ImageBitmap,
    title: String,
    isCropping: Boolean,
    onConfirm: (ProfilePictureCropRegion) -> Unit,
    onDismiss: () -> Unit
) {
    var cropRegion by remember(image) {
        mutableStateOf<ProfilePictureCropRegion?>(null)
    }

    SparrowStaticScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = FunctionalColors.MediaBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(FunctionalColors.MediaBackground),
            contentAlignment = Alignment.Center
        ) {
            ProfilePictureCropCanvas(
                image = image,
                onCropRegionChanged = { region -> cropRegion = region },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = PROFILE_PICTURE_CROP_HORIZONTAL_PADDING,
                            end = PROFILE_PICTURE_CROP_HORIZONTAL_PADDING,
                            top = PROFILE_PICTURE_CROP_TOP_PADDING,
                            bottom = PROFILE_PICTURE_CROP_BOTTOM_PADDING
                        )
            )

            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(MaterialTheme.spacing.medium),
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                SparrowRoundApprovalButton(
                    enabled = cropRegion != null && !isCropping,
                    onClick = { cropRegion?.let(onConfirm) },
                    imageVector = Icons.Filled.Check,
                    modifier = Modifier.padding(MaterialTheme.spacing.small)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfilePictureCropScreenPreview() {
    SparrowTheme {
        ProfilePictureCropScreen(
            image = ImageBitmap(512, 512),
            title = "Crop picture",
            isCropping = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

private val PROFILE_PICTURE_CROP_HORIZONTAL_PADDING = 20.dp
private val PROFILE_PICTURE_CROP_TOP_PADDING = 80.dp
private val PROFILE_PICTURE_CROP_BOTTOM_PADDING = 104.dp
