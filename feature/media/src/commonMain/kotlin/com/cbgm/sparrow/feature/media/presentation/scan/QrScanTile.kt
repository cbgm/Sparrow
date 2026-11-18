package com.cbgm.sparrow.feature.media.presentation.scan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.media.device.QrCodePreview
import com.cbgm.sparrow.feature.media.device.rememberCameraPermissionRequest
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_camera_permission_required
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrScanTile(
    label: String,
    enabled: Boolean,
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    retryCount: Int = 0
) {
    var isScanning by rememberSaveable { mutableStateOf(false) }
    val currentOnScanned by rememberUpdatedState(onScanned)
    val permissionHint = stringResource(Res.string.feature_contactimport_camera_permission_required)
    val requestPermission =
        rememberCameraPermissionRequest(
            onPermissionGranted = { isScanning = true },
            onPermissionDenied = { SparrowLog.hint(permissionHint) }
        )
    val scanning = isScanning && enabled

    LaunchedEffect(retryCount) {
        if (retryCount > 0) {
            requestPermission()
        }
    }

    ScanFrame(
        modifier = modifier.animateContentSize(),
        height = if (scanning) Dimens.ScanFrame.scanningHeight else Dimens.ScanFrame.height,
        onClick = remember(enabled, scanning, requestPermission) {
            requestPermission.takeIf { enabled && !scanning }
        }
    ) {
        if (scanning) {
            QrCodePreview(
                onScanned = { value ->
                    isScanning = false
                    currentOnScanned(value)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ScanPrompt(label = label)
        }
    }
}

@Composable
private fun ScanFrame(
    modifier: Modifier = Modifier,
    height: Dp = Dimens.ScanFrame.height,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cornerColor = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
                .drawWithContent {
                    drawContent()
                    drawViewfinderCorners(cornerColor)
                },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun ScanPrompt(
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Icon(
            imageVector = SparrowIcons.QrCode,
            contentDescription = null,
            modifier = Modifier.size(Dimens.ScanFrame.iconSize),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawViewfinderCorners(color: Color) {
    val inset = Dimens.ScanFrame.cornerInset.toPx()
    val length = Dimens.ScanFrame.cornerLength.toPx()
    val right = size.width - inset
    val bottom = size.height - inset
    val corners =
        Path().apply {
            moveTo(inset, inset + length)
            lineTo(inset, inset)
            lineTo(inset + length, inset)
            moveTo(right - length, inset)
            lineTo(right, inset)
            lineTo(right, inset + length)
            moveTo(right, bottom - length)
            lineTo(right, bottom)
            lineTo(right - length, bottom)
            moveTo(inset + length, bottom)
            lineTo(inset, bottom)
            lineTo(inset, bottom - length)
        }
    drawPath(
        path = corners,
        color = color,
        style = Stroke(width = Dimens.ScanFrame.cornerStroke.toPx())
    )
}

@Preview
@Composable
private fun QrScanTilePreview() {
    SparrowTheme {
        QrScanTile(
            label = "Scan identity QR",
            enabled = true,
            onScanned = {}
        )
    }
}
