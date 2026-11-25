package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun AttachmentBar(
    onClickGallery: () -> Unit,
    onClickCamera: () -> Unit,
    onClickFile: () -> Unit,
    onClickContact: () -> Unit,
    onClickLocation: () -> Unit,
    modifier: Modifier = Modifier,
    onClickPoll: (() -> Unit)? = null,
    onClickActivateExpenses: (() -> Unit)? = null,
    onClickCloseExpenses: (() -> Unit)? = null,
    onClickAddExpense: (() -> Unit)? = null,
    isExpensesActive: Boolean = false,
    isGalleryEnabled: Boolean = true,
    isCameraEnabled: Boolean = true,
    isFileEnabled: Boolean = true,
    isLocationInProgress: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.spacing.base + MaterialTheme.spacing.micro,
                bottom = MaterialTheme.spacing.micro
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.micro),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentButton(
                    onClick = onClickGallery,
                    imageVector = Icons.Filled.PhotoAlbum,
                    tint = MaterialTheme.attachmentColors.gallery,
                    enabled = isGalleryEnabled
                )

                AttachmentButton(
                    onClick = onClickCamera,
                    imageVector = Icons.Filled.Camera,
                    tint = MaterialTheme.attachmentColors.camera,
                    enabled = isCameraEnabled
                )
                AttachmentButton(
                    onClick = onClickFile,
                    imageVector = Icons.Filled.FilePresent,
                    tint = MaterialTheme.attachmentColors.file,
                    enabled = isFileEnabled
                )
                AttachmentButton(
                    onClick = onClickContact,
                    imageVector = Icons.Filled.Person,
                    tint = MaterialTheme.attachmentColors.contact
                )
                AttachmentButton(
                    onClick = onClickLocation,
                    imageVector = Icons.Filled.MyLocation,
                    tint = MaterialTheme.attachmentColors.location,
                    enabled = !isLocationInProgress,
                    isLoading = isLocationInProgress
                )
            }
            if (onClickPoll != null || onClickActivateExpenses != null || isExpensesActive) {
                HorizontalDivider(
                    thickness = Dimens.Base.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider),
                    modifier = Modifier.padding(
                        vertical = MaterialTheme.spacing.base,
                        horizontal = MaterialTheme.spacing.medium
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.micro),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.spacing.large,
                        alignment = Alignment.CenterHorizontally
                    )
                ) {
                    onClickPoll?.let { onClick ->
                        AttachmentButton(
                            imageVector = Icons.Filled.BarChart,
                            onClick = onClick,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val onClickToggleExpenses = if (isExpensesActive) onClickCloseExpenses else onClickActivateExpenses
                    onClickToggleExpenses?.let { onClick ->
                        AttachmentButton(
                            imageVector = if (isExpensesActive) Icons.Filled.Close else Icons.Filled.AccountBalanceWallet,
                            onClick = onClick,
                            tint = if (isExpensesActive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    if (isExpensesActive && onClickAddExpense != null) {
                        AttachmentButton(
                            imageVector = Icons.Filled.AddCircle,
                            onClick = onClickAddExpense
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.secondary,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(Dimens.AttachmentBar.buttonSize)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.AttachmentBar.iconSize),
                color = tint,
                strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
            )
        } else {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Dimens.AttachmentBar.iconSize)
            )
        }
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
            onClickLocation = {},
            onClickCamera = {},
            onClickPoll = {},
            onClickActivateExpenses = {},
            onClickCloseExpenses = {},
            onClickAddExpense = {},
            isExpensesActive = true,
            isGalleryEnabled = true,
            isCameraEnabled = true,
            isFileEnabled = true,
            isLocationInProgress = false
        )
    }
}
