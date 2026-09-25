package com.cbgm.sparrow.feature.media.presentation.camera

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType

@Composable
internal fun CameraControls(
    allowedTypes: Set<CameraCaptureType>,
    selectedType: CameraCaptureType,
    isRecording: Boolean,
    recordingDurationMilliseconds: Long,
    isFlashEnabled: Boolean,
    isFlashAvailable: Boolean,
    canSwitchCamera: Boolean,
    onDismiss: () -> Unit,
    onCaptureTypeSelected: (CameraCaptureType) -> Unit,
    onCapture: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                enabled = !isRecording,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = FunctionalColors.MediaForeground
                )
            }
            IconButton(
                onClick = onToggleFlash,
                enabled = isFlashAvailable,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = null,
                    tint = FunctionalColors.MediaForeground
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = 36.dp
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            if (isRecording) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.90f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = recordingDurationMilliseconds.toCameraDuration(),
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.small
                        ),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (allowedTypes.size > 1 && !isRecording) {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            MaterialTheme.shapes.large
                        )
                        .padding(MaterialTheme.spacing.micro),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraModeButton(
                        type = CameraCaptureType.PHOTO,
                        selected = selectedType == CameraCaptureType.PHOTO,
                        enabled = CameraCaptureType.PHOTO in allowedTypes,
                        onClick = onCaptureTypeSelected
                    )
                    CameraModeButton(
                        type = CameraCaptureType.VIDEO,
                        selected = selectedType == CameraCaptureType.VIDEO,
                        enabled = CameraCaptureType.VIDEO in allowedTypes,
                        onClick = onCaptureTypeSelected
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(CAMERA_SIDE_ACTION_WIDTH))
                CameraShutterButton(
                    type = selectedType,
                    isRecording = isRecording,
                    onClick = onCapture
                )
                Box(
                    modifier = Modifier.width(CAMERA_SIDE_ACTION_WIDTH),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onSwitchCamera,
                        enabled = canSwitchCamera && !isRecording,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = null,
                            tint = FunctionalColors.MediaForeground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModeButton(
    type: CameraCaptureType,
    selected: Boolean,
    enabled: Boolean,
    onClick: (CameraCaptureType) -> Unit
) {
    IconButton(
        enabled = enabled,
        onClick = { onClick(type) },
        modifier = Modifier.background(
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                Color.Transparent
            },
            MaterialTheme.shapes.medium
        )
    ) {
        Icon(
            imageVector =
                when (type) {
                    CameraCaptureType.PHOTO -> Icons.Filled.PhotoCamera
                    CameraCaptureType.VIDEO -> Icons.Filled.Videocam
                },
            contentDescription = null,
            tint =
                when {
                    !enabled -> FunctionalColors.MediaForeground.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> FunctionalColors.MediaForeground
                }
        )
    }
}

@Composable
private fun CameraShutterButton(
    type: CameraCaptureType,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val innerColor =
        when {
            type == CameraCaptureType.VIDEO && isRecording -> MaterialTheme.colorScheme.error
            type == CameraCaptureType.VIDEO -> MaterialTheme.colorScheme.error
            else -> FunctionalColors.MediaForeground
        }
    val innerShape = if (type == CameraCaptureType.VIDEO && isRecording) MaterialTheme.shapes.small else CircleShape

    Surface(
        modifier =
            Modifier
                .size(CAMERA_SHUTTER_SIZE)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(if (isRecording) 34.dp else 56.dp)
                        .background(innerColor, innerShape)
            )
        }
    }
}

private fun Long.toCameraDuration(): String {
    val totalSeconds = (this / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private val CAMERA_SHUTTER_SIZE = 76.dp
private val CAMERA_SIDE_ACTION_WIDTH = 76.dp

@Preview
@Composable
private fun CameraControlsPreview() {
    SparrowTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FunctionalColors.MediaBackground)
        ) {
            CameraControls(
                allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
                selectedType = CameraCaptureType.PHOTO,
                isRecording = false,
                recordingDurationMilliseconds = 0L,
                isFlashEnabled = false,
                isFlashAvailable = true,
                canSwitchCamera = true,
                onDismiss = {},
                onCaptureTypeSelected = {},
                onCapture = {},
                onSwitchCamera = {},
                onToggleFlash = {}
            )
        }
    }
}
