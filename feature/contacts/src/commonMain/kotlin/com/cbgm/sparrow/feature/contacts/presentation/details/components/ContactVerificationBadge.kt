package com.cbgm.sparrow.feature.contacts.presentation.details.components

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
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

@Composable
internal fun ContactVerificationBadge(
    icon: ImageVector,
    containerColor: Color
) {
    Surface(
        modifier = Modifier.size(Dimens.ContactDetailsScreen.verificationBadgeSize),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(Dimens.ContactDetailsScreen.verificationBadgeIconSize)
            )
        }
    }
}

@Preview
@Composable
private fun ContactVerificationBadgePreview() {
    SparrowTheme {
        ContactVerificationBadge(
            icon = Icons.Default.Security,
            containerColor = MaterialTheme.colorScheme.secondary
        )
    }
}
