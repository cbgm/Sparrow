package com.cbgm.sparrow.feature.attachments.presentation

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaSelection
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.device.CameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraLens

@Composable
fun rememberAttachmentCameraLauncher(
    selectedMedia: List<MediaSelection>,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraCaptureLauncher =
    rememberCameraCaptureLauncher(
        config =
            CameraCaptureConfig(
                allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
                initialLens = CameraLens.BACK,
                initialType = CameraCaptureType.PHOTO,
                maxImageDimension = MessageAttachmentPolicy.MAX_IMAGE_DIMENSION,
                maxImageBytes = MessageAttachmentPolicy.MAX_IMAGE_BYTES,
                maxVideoBytes = MessageAttachmentPolicy.MAX_VIDEO_BYTES.toLong()
            ),
        onCaptured = { captured ->
            if (selectedMedia.size >= MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE) {
                onError("A message can contain at most ${MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE} attachments")
            } else {
                onMediaSelected(selectedMedia + captured.toMediaSelection())
            }
        },
        onDismissed = onDismissed,
        onError = onError
    )
