package com.cbgm.sparrow.feature.media.presentation.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.cbgm.sparrow.feature.media.domain.model.ProfilePictureCropRegion
import com.cbgm.sparrow.feature.media.presentation.avatar.crop.ProfilePictureCropCanvas
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
internal fun ProfilePictureCropScreen(
    sourceBytes: ByteArray,
    title: String,
    onConfirm: (ProfilePictureCropRegion) -> Unit,
    onDismiss: () -> Unit
) {
    val image =
        remember(sourceBytes) {
            runCatching { sourceBytes.decodeToImageBitmap() }.getOrNull()
        }

    ProfilePictureCropContent(
        image = image,
        title = title,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ProfilePictureCropContent(
    image: ImageBitmap?,
    title: String,
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
                        containerColor = MaterialTheme.colorScheme.background,
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
        Box(modifier = Modifier.background(FunctionalColors.MediaBackground)) {
            image?.let {
                ProfilePictureCropCanvas(
                    image = it,
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
            }

            SparrowRoundApprovalButton(
                enabled = cropRegion != null,
                onClick = { cropRegion?.let(onConfirm) },
                imageVector = Icons.Filled.Check,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@Preview
@Composable
private fun ProfilePictureCropScreenPreview() {
    SparrowTheme {
        ProfilePictureCropContent(
            image = ImageBitmap(512, 512),
            title = "Crop picture",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

private val PROFILE_PICTURE_CROP_HORIZONTAL_PADDING = 20.dp
private val PROFILE_PICTURE_CROP_TOP_PADDING = 80.dp
private val PROFILE_PICTURE_CROP_BOTTOM_PADDING = 104.dp
