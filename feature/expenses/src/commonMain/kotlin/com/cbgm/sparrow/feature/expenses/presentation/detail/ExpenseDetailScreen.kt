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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowImage
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.asTint
import com.cbgm.sparrow.core.ui.theme.divider
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
import com.cbgm.sparrow.resources.feature_expenses_paid_by_member
import com.cbgm.sparrow.resources.feature_expenses_receipt
import com.cbgm.sparrow.resources.feature_expenses_receipt_missing
import com.cbgm.sparrow.resources.feature_expenses_receipt_view
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
            item(key = "split") { SplitSection(uiState) }
            item(key = "receipt") { ReceiptSection(uiState, onReceiptClick = { showReceipt = true }) }
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
            SparrowDialogAction(
                text = stringResource(Res.string.feature_expenses_delete_confirm),
                onClick = {
                    confirmDeletion = false
                    onUiEvent(ExpenseDetailUiEvent.DeleteConfirmed)
                },
                color = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = {
            SparrowDialogAction(
                text = stringResource(Res.string.base_cancel),
                onClick = { confirmDeletion = false },
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    SparrowTopBar(
        containerColor = containerColor,
        navigationIcon = {
            IconButton(onClick = { onUiEvent(ExpenseDetailUiEvent.BackClicked) }) {
                Icon(
                    SparrowIcons.ArrowBack,
                    contentDescription = stringResource(Res.string.base_back)
                )
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.feature_expenses_detail_title)
            )
        },
        actions = {
            if (canChange) {
                IconButton(onClick = { onUiEvent(ExpenseDetailUiEvent.EditClicked) }) {
                    Icon(
                        SparrowIcons.Edit,
                        contentDescription = stringResource(Res.string.feature_expenses_edit)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        SparrowIcons.Delete,
                        contentDescription = stringResource(Res.string.feature_expenses_delete)
                    )
                }
            }
        }
    )
}

@Composable
private fun DetailHeader(uiState: ExpenseDetailUiState) {
    val originalAmountLabel = uiState.originalAmountLabel
    val rate = uiState.rate
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.Avatar.defaultSize)
                .background(MaterialTheme.colorScheme.primary.asTint(Alpha.Button.main), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                uiState.category.icon,
                contentDescription = uiState.category.label(),
                modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = uiState.description,
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = uiState.amountLabel,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "${uiState.dateLabel} · ${stringResource(Res.string.feature_expenses_paid_by_member, uiState.payer.label())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (originalAmountLabel != null && rate != null) {
            Text(
                text = stringResource(Res.string.feature_expenses_original_amount, originalAmountLabel, rate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptSection(
    uiState: ExpenseDetailUiState,
    onReceiptClick: () -> Unit
) {
    val receiptPath = uiState.receiptPath
    Column {
        SparrowSectionLabel(text = stringResource(Res.string.feature_expenses_receipt))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = receiptPath != null, onClick = onReceiptClick)
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.GroupConversationScreen.avatarSize)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                if (receiptPath != null) {
                    SparrowImage(
                        model = receiptPath,
                        contentDescription = stringResource(Res.string.feature_expenses_receipt),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        SparrowIcons.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                text = stringResource(
                    if (receiptPath != null) Res.string.feature_expenses_receipt_view else Res.string.feature_expenses_receipt_missing
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (receiptPath != null) {
                Icon(
                    SparrowIcons.Chevron,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.divider,
            thickness = Dimens.Base.dividerThickness
        )
    }
}

@Composable
private fun SplitSection(uiState: ExpenseDetailUiState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SparrowSectionLabel(
                text = stringResource(
                    Res.string.feature_expenses_detail_split_between,
                    uiState.shares.size
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = uiState.equalShareLabel
                    ?.let { stringResource(Res.string.feature_expenses_detail_equal_each, it) }
                    ?: stringResource(Res.string.feature_expenses_split_custom_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        uiState.shares.forEach { share ->
            MemberRow(member = share.member, amountLabel = share.amountLabel)
        }
    }
}

@Composable
private fun MemberRow(
    member: ExpenseParticipantUi,
    amountLabel: String
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            ExpenseMemberAvatar(member)
            Text(
                text = member.label(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.divider,
            thickness = Dimens.Base.dividerThickness
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
