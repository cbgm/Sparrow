package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

@Composable
fun SparrowApprovalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SparrowBannerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                contentColor = textColor,
                containerColor = Color.Transparent
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SparrowSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SparrowOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SparrowRoundApprovalButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(12.dp),
        modifier = modifier.size(50.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun SparrowButtonPreview() {
    SparrowTheme {
        Column {
            SparrowApprovalButton(
                onClick = {},
                text = "Continue"
            )
            SparrowSecondaryButton(
                onClick = {},
                text = "Continue"
            )
            SparrowOutlinedButton(
                onClick = {},
                text = "Continue"
            )
            SparrowBannerButton(
                onClick = {},
                text = "Continue"
            )
            SparrowRoundApprovalButton(
                onClick = {},
                imageVector = Icons.Filled.Check
            )
        }
    }
}
