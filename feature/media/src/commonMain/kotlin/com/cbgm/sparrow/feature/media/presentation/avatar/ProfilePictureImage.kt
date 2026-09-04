package com.cbgm.sparrow.feature.media.presentation.avatar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cbgm.sparrow.core.ui.component.SparrowImage

@Composable
fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    SparrowImage(
        model = bytes,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
