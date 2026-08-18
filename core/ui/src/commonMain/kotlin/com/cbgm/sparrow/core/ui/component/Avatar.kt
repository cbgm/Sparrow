package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun SparrowAvatar(
    name: String,
    modifier: Modifier = Modifier,
    pictureBytes: ByteArray? = null,
    size: Dp = Dimens.Avatar.defaultSize
) {
    val picture =
        remember(pictureBytes) {
            pictureBytes
                ?.takeIf(ByteArray::isNotEmpty)
                ?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
        }

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
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
        SparrowAvatar(name = "Alex Example")
    }
}
