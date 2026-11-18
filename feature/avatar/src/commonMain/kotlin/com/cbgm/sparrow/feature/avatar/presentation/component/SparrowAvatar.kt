package com.cbgm.sparrow.feature.avatar.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.AvatarViewModel
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SparrowAvatar(
    name: String,
    target: AvatarTarget?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.Avatar.defaultSize,
    fallbackPainter: Painter? = null,
    ring: AvatarRing = AvatarRing.NONE
) {
    val uiState =
        if (target == null || LocalInspectionMode.current) {
            AvatarUiState.Empty
        } else {
            val viewModel =
                koinViewModel<AvatarViewModel>(key = target.viewModelKey) {
                    parametersOf(target)
                }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            state
        }

    if (ring == AvatarRing.NONE) {
        Content(
            name = name,
            uiState = uiState,
            modifier = modifier,
            size = size,
            fallbackPainter = fallbackPainter
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .avatarRing(ring),
            contentAlignment = Alignment.Center
        ) {
            Content(
                name = name,
                uiState = uiState,
                size = size - Dimens.Avatar.ringGap * 2,
                fallbackPainter = fallbackPainter
            )
        }
    }
}

@Composable
private fun Modifier.avatarRing(ring: AvatarRing): Modifier {
    val color =
        when (ring) {
            AvatarRing.VERIFIED -> MaterialTheme.colorScheme.primary
            AvatarRing.PENDING,
            AvatarRing.NONE -> MaterialTheme.colorScheme.outline
        }
    return drawBehind {
        val stroke = Dimens.Avatar.ringStroke.toPx()
        val dash = Dimens.Avatar.ringDash.toPx()
        drawCircle(
            color = color,
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)).takeIf { ring == AvatarRing.PENDING }
            )
        )
    }
}

@Composable
private fun Content(
    name: String,
    uiState: AvatarUiState,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.Avatar.defaultSize,
    fallbackPainter: Painter? = null
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.circle,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (uiState is AvatarUiState.Ready) {
                Image(
                    bitmap = uiState.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (fallbackPainter != null) {
                Icon(
                    painter = fallbackPainter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            } else {
                Text(
                    text = name.toInitials(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private val AvatarTarget.viewModelKey: String
    get() =
        when (this) {
            AvatarTarget.LocalUser -> "avatar:local-user"
            is AvatarTarget.User -> "avatar:user:$id"
            is AvatarTarget.Group -> "avatar:group:$id"
        }

private fun String.toInitials(): String =
    trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .mapNotNull { part -> part.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString(separator = "")
        .ifEmpty { "?" }

@Preview
@Composable
private fun SparrowAvatarPreview() {
    SparrowTheme {
        SparrowAvatar(name = "Alex Example", target = null)
    }
}
