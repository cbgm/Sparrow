package com.cbgm.sparrow.feature.expenses.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowImage
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowStatusBadge
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.common.component.ExpenseMemberAvatar
import com.cbgm.sparrow.feature.expenses.presentation.common.component.label
import com.cbgm.sparrow.feature.expenses.presentation.create.icon
import com.cbgm.sparrow.feature.expenses.presentation.create.label
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseDetailUiEvent
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseDetailUiState
import com.cbgm.sparrow.feature.expenses.presentation.detail.model.ExpenseShareUi
import com.cbgm.sparrow.feature.media.presentation.component.MediaViewer
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_expenses_delete
import com.cbgm.sparrow.resources.feature_expenses_delete_confirm
import com.cbgm.sparrow.resources.feature_expenses_delete_message
import com.cbgm.sparrow.resources.feature_expenses_delete_title
import com.cbgm.sparrow.resources.feature_expenses_detail_equal_each
import com.cbgm.sparrow.resources.feature_expenses_detail_split_between
import com.cbgm.sparrow.resources.feature_expenses_detail_title
import com.cbgm.sparrow.resources.feature_expenses_edit
import com.cbgm.sparrow.resources.feature_expenses_original_amount
import com.cbgm.sparrow.resources.feature_expenses_paid_by
import com.cbgm.sparrow.resources.feature_expenses_receipt
import com.cbgm.sparrow.resources.feature_expenses_receipt_missing
import com.cbgm.sparrow.resources.feature_expenses_split_custom_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseDetailScreen(
    uiState: ExpenseDetailUiState,
    onUiEvent: (ExpenseDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDeletion by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }

    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { color ->
            DetailTopBar(
                canChange = uiState.canChange,
                containerColor = color,
                onUiEvent = onUiEvent,
                onDeleteClick = { confirmDeletion = true }
            )
        }
    ) { innerPadding, listState ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@SparrowLazyScaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(
                horizontal = MaterialTheme.spacing.screenPadding
            ),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            item(key = "header") { DetailHeader(uiState) }
            item(key = "payment") { PaymentCard(uiState, onReceiptClick = { showReceipt = true }) }
            item(key = "split") { SplitSection(uiState) }
        }
    }

    val receiptPath = uiState.receiptPath
    if (showReceipt && receiptPath != null) {
        MediaViewer(
            media = listOf(
                MediaItemUi(
                    id = receiptPath,
                    type = MediaTypeUi.IMAGE,
                    mimeType = RECEIPT_MIME_TYPE,
                    localFilePath = receiptPath
                )
            ),
            onDismiss = { showReceipt = false },
            title = { _, _ -> uiState.description }
        )
    }

    SparrowAlertDialog(
        isVisible = confirmDeletion,
        onDismissRequest = { confirmDeletion = false },
        confirmButton = {
            SparrowDestructiveButton(
                text = stringResource(Res.string.feature_expenses_delete_confirm),
                onClick = {
                    confirmDeletion = false
                    onUiEvent(ExpenseDetailUiEvent.DeleteConfirmed)
                },
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                text = stringResource(Res.string.base_cancel),
                onClick = { confirmDeletion = false },
                fillMaxWidth = false
            )
        },
        title = stringResource(Res.string.feature_expenses_delete_title),
        text = {
            Text(
                stringResource(
                    Res.string.feature_expenses_delete_message,
                    uiState.description
                )
            )
        }
    )
}

@Composable
private fun DetailTopBar(
    canChange: Boolean,
    containerColor: Color,
    onUiEvent: (ExpenseDetailUiEvent) -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        navigationIcon = {
            IconButton(onClick = { onUiEvent(ExpenseDetailUiEvent.BackClicked) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.feature_expenses_detail_title),
                style = MaterialTheme.typography.titleSmall
            )
        },
        actions = {
            if (canChange) {
                IconButton(onClick = { onUiEvent(ExpenseDetailUiEvent.EditClicked) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.feature_expenses_edit)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(Res.string.feature_expenses_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun DetailHeader(uiState: ExpenseDetailUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.Expense.detailIconSize)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                uiState.category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = uiState.description,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = uiState.amountLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        val originalAmountLabel = uiState.originalAmountLabel
        val rate = uiState.rate
        if (originalAmountLabel != null && rate != null) {
            Text(
                text = stringResource(
                    Res.string.feature_expenses_original_amount,
                    originalAmountLabel,
                    rate
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SparrowStatusBadge(
            text = uiState.category.label(),
            icon = uiState.category.icon,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PaymentCard(
    uiState: ExpenseDetailUiState,
    onReceiptClick: () -> Unit
) {
    SparrowCardNoAnimation {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                ExpenseMemberAvatar(uiState.payer)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.feature_expenses_paid_by),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.payer.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = uiState.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = Dimens.Base.dividerThickness
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = uiState.receiptPath != null, onClick = onReceiptClick)
                    .padding(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.GroupConversationScreen.avatarSize)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    val receiptPath = uiState.receiptPath
                    if (receiptPath != null) {
                        SparrowImage(
                            model = receiptPath,
                            contentDescription = stringResource(Res.string.feature_expenses_receipt),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.MessageBubble.iconSize)
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.feature_expenses_receipt),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.receiptPath == null) {
                    Text(
                        text = stringResource(Res.string.feature_expenses_receipt_missing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitSection(uiState: ExpenseDetailUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    Res.string.feature_expenses_detail_split_between,
                    uiState.shares.size
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = uiState.equalShareLabel
                    ?.let { stringResource(Res.string.feature_expenses_detail_equal_each, it) }
                    ?: stringResource(Res.string.feature_expenses_split_custom_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SparrowCardNoAnimation {
            Column {
                uiState.shares.forEachIndexed { index, share ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = Dimens.Base.dividerThickness
                        )
                    }
                    ShareRow(share)
                }
            }
        }
    }
}

@Composable
private fun ShareRow(share: ExpenseShareUi) {
    val contentColor = if (share.member.isLocal) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (share.member.isLocal) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.base
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        ExpenseMemberAvatar(share.member)
        Text(
            text = share.member.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (share.member.isLocal) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = share.amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun ExpenseDetailScreenPreview() {
    val you = ExpenseParticipantUi("you", "Chris", isLocal = true)
    val alex = ExpenseParticipantUi("alex", "Alex")
    SparrowTheme {
        ExpenseDetailScreen(
            uiState = ExpenseDetailUiState(
                isLoading = false,
                description = "Hotel in Rome",
                amountLabel = "€650.00",
                category = ExpenseCategoryUi.ACCOMMODATION,
                payer = alex,
                dateLabel = "12.06.26",
                shares = listOf(
                    ExpenseShareUi(you, "€325.00"),
                    ExpenseShareUi(alex, "€325.00")
                ),
                equalShareLabel = "€325.00",
                canChange = true
            ),
            onUiEvent = {}
        )
    }
}

private const val RECEIPT_MIME_TYPE = "image/jpeg"
