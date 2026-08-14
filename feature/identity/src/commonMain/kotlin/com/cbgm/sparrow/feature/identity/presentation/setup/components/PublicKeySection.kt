package com.cbgm.sparrow.feature.identity.presentation.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.extensions.toHexString
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

private val Field = Color(0xFF102A46)

@Composable
fun PublicKeySection(
    icon: ImageVector,
    title: String,
    description: String,
    key: ByteArray
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = key.toHexString(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = Field,
                        shape = MaterialTheme.shapes.medium
                    ).padding(MaterialTheme.spacing.base),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Composable
fun PublicKeySectionPreview() {
    SparrowTheme {
        PublicKeySection(
            icon = Icons.Default.VerifiedUser,
            title = "Test",
            description = "Test",
            key = ByteArray(32)
        )
    }
}
