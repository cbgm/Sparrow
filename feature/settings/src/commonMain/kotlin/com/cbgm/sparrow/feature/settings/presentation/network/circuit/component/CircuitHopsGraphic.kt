package com.cbgm.sparrow.feature.settings.presentation.network.circuit.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitHopsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.circuit.model.CircuitUiStatus
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_contact
import com.cbgm.sparrow.resources.feature_chats_you
import org.jetbrains.compose.resources.stringResource

@Composable
fun CircuitHopsGraphic(
    uiState: CircuitHopsUiState,
    modifier: Modifier = Modifier
) {
    CircuitHopsGraphic(
        hopCount = uiState.selectedHopCount,
        circuitCount = uiState.selectedCircuitCount,
        decoyCount = uiState.selectedDecoyCount,
        sendsDirectlyWithoutCircuit = uiState.sendsDirectlyWithoutCircuit,
        status = uiState.status,
        modifier = modifier
    )
}

@Composable
fun CircuitHopsGraphic(
    hopCount: Int,
    circuitCount: Int,
    decoyCount: Int,
    sendsDirectlyWithoutCircuit: Boolean,
    status: CircuitUiStatus,
    modifier: Modifier = Modifier
) {
    val youLabel = stringResource(Res.string.feature_chats_you)
    val contactLabel = stringResource(Res.string.base_contact)

    val time by rememberInfiniteTransition(label = "circuit_hops_anim").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circuit_hops_time"
    )

    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val pathColor = MaterialTheme.colorScheme.outline
    val nodeColor = MaterialTheme.colorScheme.outline
    val solidNodeColor = MaterialTheme.colorScheme.onBackground
    val packetColor = MaterialTheme.colorScheme.primary
    val directColor = Color(0xFFFFC107)

    val isDirectFallback = (status is CircuitUiStatus.Unavailable) && sendsDirectlyWithoutCircuit

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = MaterialTheme.spacing.small)
    ) {
        val width = size.width
        val height = size.height

        val labelLayoutYou = measurer.measure(youLabel, labelStyle)
        val labelLayoutContact = measurer.measure(contactLabel, labelStyle)

        val topInset = labelLayoutYou.size.height + 12.dp.toPx()
        val bottomInset = 16.dp.toPx()
        val usableHeight = height - topInset - bottomInset
        val centerY = topInset + usableHeight / 2f

        val leftX = 24.dp.toPx() + labelLayoutYou.size.width / 2f
        val rightX = width - 24.dp.toPx() - labelLayoutContact.size.width / 2f

        val startPoint = Offset(leftX, centerY)
        val endPoint = Offset(rightX, centerY)

        // Draw start node ("You") label and circle
        drawCircle(
            color = solidNodeColor,
            radius = 5.dp.toPx(),
            center = startPoint
        )
        drawText(
            textLayoutResult = labelLayoutYou,
            topLeft = Offset(startPoint.x - labelLayoutYou.size.width / 2f, startPoint.y - topInset)
        )

        // Draw end node ("Contact") label and circle
        drawCircle(
            color = solidNodeColor,
            radius = 5.dp.toPx(),
            center = endPoint
        )
        drawText(
            textLayoutResult = labelLayoutContact,
            topLeft = Offset(endPoint.x - labelLayoutContact.size.width / 2f, endPoint.y - topInset)
        )

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))

        if (isDirectFallback) {
            // Draw single yellow dashed line straight from You to Contact
            val directPath = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                lineTo(endPoint.x, endPoint.y)
            }
            drawPath(
                path = directPath,
                color = directColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = dashEffect
                )
            )

            // Animated packet moving along direct path
            val packetX = startPoint.x + (endPoint.x - startPoint.x) * time
            val packetY = startPoint.y + (endPoint.y - startPoint.y) * time
            drawCircle(
                color = directColor,
                radius = 4.dp.toPx(),
                center = Offset(packetX, packetY)
            )
        } else {
            // Calculate paths for each circuit
            val numCircuits = circuitCount.coerceIn(1, 3)
            val numHops = hopCount.coerceIn(1, 5)

            // Vertical spreads for multiple circuits
            val circuitOffsetsY = when (numCircuits) {
                1 -> listOf(0f)
                2 -> listOf(-28.dp.toPx(), 28.dp.toPx())
                else -> listOf(-36.dp.toPx(), 0f, 36.dp.toPx())
            }

            for (c in 0 until numCircuits) {
                val circuitBaseY = centerY + circuitOffsetsY[c]
                val points = mutableListOf<Offset>()
                points.add(startPoint)

                // Generate relay points for this circuit
                val stepX = (rightX - leftX) / (numHops + 1)
                for (h in 1..numHops) {
                    val nodeX = leftX + stepX * h
                    // Zigzag direction alternates
                    val amplitude = if (numCircuits > 1) 12.dp.toPx() else 18.dp.toPx()
                    val direction = if (h % 2 == 1) -1f else 1f
                    val nodeY = circuitBaseY + direction * amplitude
                    points.add(Offset(nodeX, nodeY))
                }
                points.add(endPoint)

                // Build path for circuit c
                val circuitPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(
                    path = circuitPath,
                    color = pathColor.copy(alpha = 0.7f),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )

                // Draw relay open circles
                for (i in 1 until points.size - 1) {
                    drawCircle(
                        color = nodeColor,
                        radius = 4.dp.toPx(),
                        center = points[i],
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                }

                // Animate packet along circuit path
                val totalSegments = points.size - 1
                val segmentProgress = (time * totalSegments) % totalSegments
                val segIndex = segmentProgress.toInt().coerceIn(0, totalSegments - 1)
                val segT = segmentProgress - segIndex

                val p1 = points[segIndex]
                val p2 = points[segIndex + 1]
                val packetPos = Offset(
                    p1.x + (p2.x - p1.x) * segT,
                    p1.y + (p2.y - p1.y) * segT
                )

                drawCircle(
                    color = packetColor,
                    radius = 3.5.dp.toPx(),
                    center = packetPos
                )
            }

            // Draw Decoy Routes if decoyCount > 0
            val numDecoys = decoyCount.coerceIn(0, 5)
            if (numDecoys > 0) {
                val decoyAlpha = 0.35f
                val decoyPathColor = pathColor.copy(alpha = decoyAlpha)

                for (d in 0 until numDecoys) {
                    // Decoys launch off to secondary mailboxes
                    val targetX = rightX - 10.dp.toPx()
                    val decoyYOffset = when (d % 3) {
                        0 -> -50.dp.toPx() - (d * 8).dp.toPx()
                        1 -> 50.dp.toPx() + (d * 8).dp.toPx()
                        else -> (if (d % 2 == 0) -30.dp.toPx() else 30.dp.toPx())
                    }
                    val targetY = (centerY + decoyYOffset).coerceIn(topInset + 10.dp.toPx(), height - bottomInset - 10.dp.toPx())

                    val decoyRelay1 = Offset(
                        leftX + (targetX - leftX) * 0.35f,
                        centerY + (targetY - centerY) * 0.3f + (if (d % 2 == 0) -12.dp.toPx() else 12.dp.toPx())
                    )
                    val decoyRelay2 = Offset(
                        leftX + (targetX - leftX) * 0.70f,
                        centerY + (targetY - centerY) * 0.7f + (if (d % 2 == 0) 10.dp.toPx() else -10.dp.toPx())
                    )
                    val decoyTarget = Offset(targetX, targetY)

                    val decoyPoints = listOf(startPoint, decoyRelay1, decoyRelay2, decoyTarget)

                    val decoyPath = Path().apply {
                        moveTo(decoyPoints[0].x, decoyPoints[0].y)
                        for (i in 1 until decoyPoints.size) {
                            lineTo(decoyPoints[i].x, decoyPoints[i].y)
                        }
                    }

                    drawPath(
                        path = decoyPath,
                        color = decoyPathColor,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                        )
                    )

                    // Decoy relay nodes
                    for (i in 1..2) {
                        drawCircle(
                            color = nodeColor.copy(alpha = decoyAlpha),
                            radius = 3.5.dp.toPx(),
                            center = decoyPoints[i],
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Decoy end mailbox node
                    drawCircle(
                        color = nodeColor.copy(alpha = decoyAlpha + 0.15f),
                        radius = 4.dp.toPx(),
                        center = decoyTarget,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Decoy packet
                    val totalSegments = decoyPoints.size - 1
                    val decoyTime = (time + d * 0.2f) % 1f
                    val segmentProgress = (decoyTime * totalSegments) % totalSegments
                    val segIndex = segmentProgress.toInt().coerceIn(0, totalSegments - 1)
                    val segT = segmentProgress - segIndex

                    val p1 = decoyPoints[segIndex]
                    val p2 = decoyPoints[segIndex + 1]
                    val decoyPacketPos = Offset(
                        p1.x + (p2.x - p1.x) * segT,
                        p1.y + (p2.y - p1.y) * segT
                    )

                    drawCircle(
                        color = packetColor.copy(alpha = decoyAlpha + 0.2f),
                        radius = 2.8.dp.toPx(),
                        center = decoyPacketPos
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CircuitHopsGraphic1RelayPreview() {
    SparrowTheme {
        CircuitHopsGraphic(
            hopCount = 1,
            circuitCount = 1,
            decoyCount = 0,
            sendsDirectlyWithoutCircuit = false,
            status = CircuitUiStatus.Building
        )
    }
}

@Preview
@Composable
private fun CircuitHopsGraphic3RelaysPreview() {
    SparrowTheme {
        CircuitHopsGraphic(
            hopCount = 3,
            circuitCount = 1,
            decoyCount = 0,
            sendsDirectlyWithoutCircuit = false,
            status = CircuitUiStatus.Building
        )
    }
}

@Preview
@Composable
private fun CircuitHopsGraphic2CircuitsPreview() {
    SparrowTheme {
        CircuitHopsGraphic(
            hopCount = 3,
            circuitCount = 2,
            decoyCount = 0,
            sendsDirectlyWithoutCircuit = false,
            status = CircuitUiStatus.Building
        )
    }
}

@Preview
@Composable
private fun CircuitHopsGraphic3Circuits2DecoysPreview() {
    SparrowTheme {
        CircuitHopsGraphic(
            hopCount = 3,
            circuitCount = 3,
            decoyCount = 2,
            sendsDirectlyWithoutCircuit = false,
            status = CircuitUiStatus.Building
        )
    }
}

@Preview
@Composable
private fun CircuitHopsGraphicDirectSendPreview() {
    SparrowTheme {
        CircuitHopsGraphic(
            hopCount = 3,
            circuitCount = 1,
            decoyCount = 0,
            sendsDirectlyWithoutCircuit = true,
            status = CircuitUiStatus.Unavailable
        )
    }
}
