package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun AttachmentBar(
    onClickGallery: () -> Unit,
    onClickCamera: () -> Unit,
    onClickFile: () -> Unit,
    onClickContact: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = MaterialTheme.spacing.screenPadding
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilledButton(
            onClick = onClickGallery,
            imageVector = Icons.Filled.PhotoAlbum,
            tint = MaterialTheme.attachmentColors.gallery
        )

        FilledButton(
            onClick = onClickCamera,
            imageVector = Icons.Filled.Camera,
            tint = MaterialTheme.attachmentColors.camera
        )
        FilledButton(
            onClick = onClickFile,
            imageVector = Icons.Filled.FilePresent,
            tint = MaterialTheme.attachmentColors.file
        )
        FilledButton(
            onClick = onClickContact,
            imageVector = Icons.Filled.Person,
            tint = MaterialTheme.attachmentColors.contact
        )
    }
}

@Composable
private fun FilledButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.secondary
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Preview
@Composable
private fun AttachmentBarPreview() {
    SparrowTheme {
        AttachmentBar(
            onClickGallery = {},
            onClickContact = {},
            onClickFile = {},
            onClickCamera = {}
        )
    }
}
