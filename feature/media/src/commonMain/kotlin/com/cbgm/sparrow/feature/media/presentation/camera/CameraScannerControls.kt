package com.cbgm.sparrow.feature.media.presentation.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.DocumentCaptureMode
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_document_scanner_automatic
import com.cbgm.sparrow.resources.feature_media_document_scanner_manual
import com.cbgm.sparrow.resources.feature_media_document_scanner_test_image
import org.jetbrains.compose.resources.stringResource

internal enum class ScanGuideShape(
    val aspectRatio: Float,
    val widthFraction: Float
) {
    QR_CODE(aspectRatio = 1f, widthFraction = 0.68f),
    DOCUMENT(aspectRatio = 1.5f, widthFraction = 0.78f)
}

internal data class DocumentCaptureControls(
    val mode: DocumentCaptureMode,
    val isCapturing: Boolean,
    val onModeSelected: (DocumentCaptureMode) -> Unit,
    val onCapture: () -> Unit,
    val onPickTestImage: (() -> Unit)? = null
)

@Composable
internal fun CameraScannerControls(
    guideShape: ScanGuideShape,
    hint: String,
    isHighlighted: Boolean,
    isFlashEnabled: Boolean,
    isFlashAvailable: Boolean,
    onDismiss: () -> Unit,
    onToggleFlash: () -> Unit,
    modifier: Modifier = Modifier,
    capture: DocumentCaptureControls? = null
) {
    val isCapturing = capture?.isCapturing == true
    val frameColor by animateColorAsState(
        if (isHighlighted) MaterialTheme.colorScheme.primary else FunctionalColors.MediaForeground
    )

    Box(modifier = modifier.fillMaxSize()) {
        ScanGuideFrame(
            shape = guideShape,
            color = frameColor,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScannerIconButton(onClick = onDismiss, enabled = !isCapturing) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.base_close),
                    tint = FunctionalColors.MediaForeground
                )
            }
            ScannerIconButton(onClick = onToggleFlash, enabled = isFlashAvailable && !isCapturing) {
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.large
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = hint,
                modifier =
                    Modifier
                        .background(
                            controlBackground(),
                            MaterialTheme.shapes.medium
                        ).padding(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.small
                        ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            if (capture != null) {
                DocumentCaptureRow(capture = capture)
            }
        }
    }
}

@Composable
private fun DocumentCaptureRow(capture: DocumentCaptureControls) {
    CaptureModeSelection(
        mode = capture.mode,
        enabled = !capture.isCapturing,
        onModeSelected = capture.onModeSelected
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.width(SIDE_ACTION_WIDTH),
            contentAlignment = Alignment.CenterStart
        ) {
            capture.onPickTestImage?.let { onPickTestImage ->
                ScannerIconButton(onClick = onPickTestImage, enabled = !capture.isCapturing) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription =
                            stringResource(Res.string.feature_media_document_scanner_test_image),
                        tint = FunctionalColors.MediaForeground
                    )
                }
            }
        }
        Box(contentAlignment = Alignment.Center) {
            CameraShutterButton(
                type = CameraCaptureType.PHOTO,
                isRecording = false,
                onClick = { if (!capture.isCapturing) capture.onCapture() }
            )
            if (capture.isCapturing) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.width(SIDE_ACTION_WIDTH))
    }
}

@Composable
private fun CaptureModeSelection(
    mode: DocumentCaptureMode,
    enabled: Boolean,
    onModeSelected: (DocumentCaptureMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.background(controlBackground(), CircleShape)
    ) {
        DocumentCaptureMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onModeSelected(entry) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, DocumentCaptureMode.entries.size),
                label = {
                    Text(
                        text = stringResource(
                            if (entry == DocumentCaptureMode.AUTOMATIC) {
                                Res.string.feature_media_document_scanner_automatic
                            } else {
                                Res.string.feature_media_document_scanner_manual
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ScannerIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.background(controlBackground(), CircleShape),
        content = content
    )
}

@Composable
private fun ScanGuideFrame(
    shape: ScanGuideShape,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val frameWidth = size.width * shape.widthFraction
        val frameHeight =
            (frameWidth * shape.aspectRatio).coerceAtMost(size.height * GUIDE_MAX_HEIGHT_FRACTION)
        val topLeft =
            Offset(
                x = (size.width - frameWidth) / 2f,
                y = (size.height - frameHeight) / 2f - GUIDE_VERTICAL_SHIFT.toPx()
            )
        val corner = CornerRadius(GUIDE_CORNER_RADIUS.toPx())
        val guide = RoundRect(Rect(topLeft, Size(frameWidth, frameHeight)), corner)
        val scrim =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(guide)
            }
        drawPath(scrim, FunctionalColors.MediaBackground.copy(alpha = GUIDE_SCRIM_ALPHA))
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = Size(frameWidth, frameHeight),
            cornerRadius = corner,
            style = Stroke(width = GUIDE_STROKE_WIDTH.toPx())
        )
    }
}

@Composable
private fun controlBackground(): Color = MaterialTheme.colorScheme.surface.copy(alpha = CONTROL_ALPHA)

private const val CONTROL_ALPHA = 0.78f
private val SIDE_ACTION_WIDTH = 76.dp
private const val GUIDE_MAX_HEIGHT_FRACTION = 0.6f
private const val GUIDE_SCRIM_ALPHA = 0.45f
private val GUIDE_VERTICAL_SHIFT = 48.dp
private val GUIDE_CORNER_RADIUS = 16.dp
private val GUIDE_STROKE_WIDTH = 3.dp

@Preview
@Composable
private fun DocumentScannerControlsPreview() {
    SparrowTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FunctionalColors.MediaBackground)
        ) {
            CameraScannerControls(
                guideShape = ScanGuideShape.DOCUMENT,
                hint = "Place the document inside the frame",
                isHighlighted = true,
                isFlashEnabled = false,
                isFlashAvailable = true,
                onDismiss = {},
                onToggleFlash = {},
                capture =
                    DocumentCaptureControls(
                        mode = DocumentCaptureMode.AUTOMATIC,
                        isCapturing = false,
                        onModeSelected = {},
                        onCapture = {},
                        onPickTestImage = {}
                    )
            )
        }
    }
}

@Preview
@Composable
private fun QrScannerControlsPreview() {
    SparrowTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FunctionalColors.MediaBackground)
        ) {
            CameraScannerControls(
                guideShape = ScanGuideShape.QR_CODE,
                hint = "Point the camera at a QR code",
                isHighlighted = false,
                isFlashEnabled = false,
                isFlashAvailable = true,
                onDismiss = {},
                onToggleFlash = {}
            )
        }
    }
}
