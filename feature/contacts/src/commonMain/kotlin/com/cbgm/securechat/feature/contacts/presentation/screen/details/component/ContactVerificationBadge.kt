package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme

@Composable
internal fun ContactVerificationBadge(
    icon: ImageVector,
    containerColor: Color
) {
    Surface(
        modifier = Modifier.size(26.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ContactVerificationBadgePreview() {
    SecureChatTheme {
        ContactVerificationBadge(
            icon = Icons.Default.Security,
            containerColor = MaterialTheme.colorScheme.secondary
        )
    }
}
