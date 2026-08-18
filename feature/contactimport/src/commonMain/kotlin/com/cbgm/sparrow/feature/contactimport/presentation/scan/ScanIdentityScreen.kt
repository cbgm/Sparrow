package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.scanIdentityScreen
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contactimport.device.QrScanner
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_scan_identity
import com.cbgm.sparrow.resources.feature_contactimport_scan_identity_instruction
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanIdentityScreen(
    onUiEvent: (ScanIdentityUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowStaticScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                title = {
                    Text(
                        text = stringResource(Res.string.feature_contactimport_scan_identity),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(ScanIdentityUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            QrScanner(
                onQrCodeScanned = { encodedIdentity ->
                    onUiEvent(ScanIdentityUiEvent.QrCodeScanned(encodedIdentity))
                },
                modifier = Modifier.fillMaxSize()
            )

            ScannerOverlay(
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = stringResource(Res.string.feature_contactimport_scan_identity_instruction),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.screenPadding,
                            vertical = MaterialTheme.spacing.times(5)
                        ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Draws a scrim with a transparent square cutout in the center and
// bracket-style corners around it — the standard "viewfinder" affordance
// that tells the user exactly where to aim, instead of a bare camera feed.
@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val accentColor = MaterialTheme.colorScheme.secondary

    val frameCornerRadius = MaterialTheme.shapes.scanIdentityScreen.frameCornerRadius

    Canvas(modifier = modifier) {
        val frameSize = size.minDimension * 0.62f
        val left = (size.width - frameSize) / 2f
        val top =
            (size.height - frameSize) / 2f - Dimens.ScanIdentityScreen.frameVerticalOffset.toPx()

        val scrimPath =
            Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    RoundRect(
                        rect =
                            Rect(
                                offset = Offset(left, top),
                                size = Size(frameSize, frameSize)
                            ),
                        cornerRadius = CornerRadius(frameCornerRadius.toPx())
                    )
                )
                fillType = PathFillType.EvenOdd
            }

        drawPath(path = scrimPath, color = backgroundColor.copy(alpha = 0.65f))

        val cornerLength = Dimens.ScanIdentityScreen.frameCornerLength.toPx()
        val strokeWidth = Dimens.ScanIdentityScreen.frameStrokeWidth.toPx()
        val corners =
            listOf(
                Offset(left, top) to Pair(1, 1),
                Offset(left + frameSize, top) to Pair(-1, 1),
                Offset(left, top + frameSize) to Pair(1, -1),
                Offset(left + frameSize, top + frameSize) to Pair(-1, -1)
            )

        corners.forEach { (corner, direction) ->
            val (dx, dy) = direction
            drawLine(
                color = accentColor,
                start = corner,
                end = Offset(corner.x + cornerLength * dx, corner.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor,
                start = corner,
                end = Offset(corner.x, corner.y + cornerLength * dy),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview
@Composable
fun ScannedIdentityPreview() {
    SparrowTheme {
        ScanIdentityScreen(
            onUiEvent = {}
        )
    }
}
