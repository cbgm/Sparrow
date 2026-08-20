package com.cbgm.sparrow.feature.safety.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.animation.rememberHighlightColor
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningReason
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_safety_block_confirm
import com.cbgm.sparrow.resources.feature_safety_block_confirm_description
import com.cbgm.sparrow.resources.feature_safety_block_error
import com.cbgm.sparrow.resources.feature_safety_block_this_user
import com.cbgm.sparrow.resources.feature_safety_blocking_user
import com.cbgm.sparrow.resources.feature_safety_details_detected_reasons
import com.cbgm.sparrow.resources.feature_safety_details_learn_more
import com.cbgm.sparrow.resources.feature_safety_details_title
import com.cbgm.sparrow.resources.feature_safety_details_why_flagged
import com.cbgm.sparrow.resources.feature_safety_high_risk_message
import com.cbgm.sparrow.resources.feature_safety_high_risk_summary
import com.cbgm.sparrow.resources.feature_safety_reason_credential_request
import com.cbgm.sparrow.resources.feature_safety_reason_credential_request_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_ip_address_link
import com.cbgm.sparrow.resources.feature_safety_reason_ip_address_link_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_lookalike_domain
import com.cbgm.sparrow.resources.feature_safety_reason_lookalike_domain_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_mixed_script_domain
import com.cbgm.sparrow.resources.feature_safety_reason_mixed_script_domain_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_payment_request
import com.cbgm.sparrow.resources.feature_safety_reason_payment_request_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_private_key_request
import com.cbgm.sparrow.resources.feature_safety_reason_private_key_request_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_suspicious_link
import com.cbgm.sparrow.resources.feature_safety_reason_suspicious_link_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_urgent_action_request
import com.cbgm.sparrow.resources.feature_safety_reason_urgent_action_request_explanation
import com.cbgm.sparrow.resources.feature_safety_reason_url_shortener
import com.cbgm.sparrow.resources.feature_safety_reason_url_shortener_explanation
import com.cbgm.sparrow.resources.feature_safety_suspicious_message
import com.cbgm.sparrow.resources.feature_safety_suspicious_summary
import com.cbgm.sparrow.resources.feature_safety_user_blocked
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private const val SAFETY_HIGHLIGHT_DURATION = 2_00

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSafetyDetailsScreen(
    level: MessageSafetyWarningLevel,
    reasons: List<MessageSafetyWarningReason>,
    focusReason: MessageSafetyWarningReason?,
    canBlockUser: Boolean,
    isUserBlocked: Boolean,
    isBlockingUser: Boolean,
    blockError: String?,
    onBackClick: () -> Unit,
    onBlockUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detectedReasons = reasons.distinct()
    val allReasons = MessageSafetyWarningReason.entries
    val explanationStartIndex = detectedReasons.size + 3
    var highlightedReason by remember(focusReason) { mutableStateOf(focusReason) }
    var showBlockConfirmation by remember { mutableStateOf(false) }

    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.feature_safety_details_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.base_back)
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        scrolledContainerColor = containerColor,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
            )
        },
        bottomBar = { containerColor ->
            if (canBlockUser) {
                BlockUserBottomBar(
                    isBlocked = isUserBlocked,
                    isBlocking = isBlockingUser,
                    error = blockError,
                    onClick = { showBlockConfirmation = true },
                    containerColor = containerColor
                )
            }
        }
    ) { innerPadding, listState ->
        LaunchedEffect(highlightedReason, detectedReasons) {
            val reason = highlightedReason ?: return@LaunchedEffect
            val focusIndex = allReasons.indexOf(reason)
            if (focusIndex >= 0) {
                listState.animateScrollToItem(explanationStartIndex + focusIndex)
                delay(SAFETY_HIGHLIGHT_DURATION.milliseconds)
                if (highlightedReason == reason) {
                    highlightedReason = null
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding
        ) {
            item(key = "summary") {
                SafetySummary(
                    level = level,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.medium)
                            .padding(top = MaterialTheme.spacing.small)
                )
            }

            if (detectedReasons.isNotEmpty()) {
                item(key = "detected-reasons-title") {
                    SectionTitle(
                        text = stringResource(Res.string.feature_safety_details_detected_reasons),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
                    )
                }

                items(
                    items = detectedReasons,
                    key = { reason -> "detected-${reason.id}" }
                ) { reason ->
                    ReasonLink(
                        reason = reason,
                        onClick = { highlightedReason = reason }
                    )
                }
            }

            item(key = "learn-more-title") {
                SectionTitle(
                    text = stringResource(Res.string.feature_safety_details_learn_more),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
                )
            }

            items(
                items = allReasons,
                key = { reason -> "explanation-${reason.id}" }
            ) { reason ->
                ReasonExplanation(
                    reason = reason,
                    isHighlighted = reason == highlightedReason
                )
            }
        }
    }

    if (showBlockConfirmation) {
        SparrowAlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = stringResource(Res.string.feature_safety_block_confirm),
            text = {
                Text(
                    text = stringResource(Res.string.feature_safety_block_confirm_description),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                SparrowDestructiveButton(
                    onClick = {
                        showBlockConfirmation = false
                        onBlockUserClick()
                    },
                    text = stringResource(Res.string.feature_safety_block_this_user),
                    fillMaxWidth = false
                )
            },
            dismissButton = {
                SparrowOutlinedButton(
                    onClick = { showBlockConfirmation = false },
                    text = stringResource(Res.string.base_cancel),
                    fillMaxWidth = false
                )
            }
        )
    }
}

@Composable
private fun SafetySummary(
    level: MessageSafetyWarningLevel,
    modifier: Modifier = Modifier
) {
    val title =
        when (level) {
            MessageSafetyWarningLevel.SUSPICIOUS ->
                stringResource(Res.string.feature_safety_suspicious_message)

            MessageSafetyWarningLevel.HIGH ->
                stringResource(Res.string.feature_safety_high_risk_message)
        }
    val summary =
        when (level) {
            MessageSafetyWarningLevel.SUSPICIOUS ->
                stringResource(Res.string.feature_safety_suspicious_summary)

            MessageSafetyWarningLevel.HIGH ->
                stringResource(Res.string.feature_safety_high_risk_summary)
        }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Dimens.MessageSafetyWarning.iconSize)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(Res.string.feature_safety_details_why_flagged),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = Alpha.OpaqueText)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.base),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ReasonLink(
    reason: MessageSafetyWarningReason,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(reasonTitle(reason)),
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.base
                ),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary
    )
    HorizontalDivider(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.medium),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
    )
}

@Composable
private fun ReasonExplanation(
    reason: MessageSafetyWarningReason,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        rememberHighlightColor(
            isHighlighted = isHighlighted,
            baseColor = Color.Transparent,
            highlightColor = MaterialTheme.colorScheme.errorContainer // .copy(alpha = Alpha.Subtle)
        )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.medium),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            Text(
                text = stringResource(reasonTitle(reason)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(reasonExplanation(reason)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
            )
        }
    }
}

@Composable
private fun BlockUserBottomBar(
    isBlocked: Boolean,
    isBlocking: Boolean,
    error: String?,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            if (isBlocked) {
                Text(
                    text = stringResource(Res.string.feature_safety_user_blocked),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
                )
            } else {
                SparrowDestructiveButton(
                    onClick = onClick,
                    text =
                        if (isBlocking) {
                            stringResource(Res.string.feature_safety_blocking_user)
                        } else {
                            stringResource(Res.string.feature_safety_block_this_user)
                        },
                    enabled = !isBlocking
                )
            }

            if (error != null) {
                Text(
                    text = stringResource(Res.string.feature_safety_block_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun reasonTitle(reason: MessageSafetyWarningReason): StringResource =
    when (reason) {
        MessageSafetyWarningReason.SUSPICIOUS_LINK -> Res.string.feature_safety_reason_suspicious_link
        MessageSafetyWarningReason.LOOKALIKE_DOMAIN -> Res.string.feature_safety_reason_lookalike_domain
        MessageSafetyWarningReason.MIXED_SCRIPT_DOMAIN -> Res.string.feature_safety_reason_mixed_script_domain
        MessageSafetyWarningReason.IP_ADDRESS_LINK -> Res.string.feature_safety_reason_ip_address_link
        MessageSafetyWarningReason.URL_SHORTENER -> Res.string.feature_safety_reason_url_shortener
        MessageSafetyWarningReason.URGENT_ACTION_REQUEST -> Res.string.feature_safety_reason_urgent_action_request
        MessageSafetyWarningReason.CREDENTIAL_REQUEST -> Res.string.feature_safety_reason_credential_request
        MessageSafetyWarningReason.PAYMENT_REQUEST -> Res.string.feature_safety_reason_payment_request
        MessageSafetyWarningReason.PRIVATE_KEY_REQUEST -> Res.string.feature_safety_reason_private_key_request
    }

private fun reasonExplanation(reason: MessageSafetyWarningReason): StringResource =
    when (reason) {
        MessageSafetyWarningReason.SUSPICIOUS_LINK -> Res.string.feature_safety_reason_suspicious_link_explanation
        MessageSafetyWarningReason.LOOKALIKE_DOMAIN -> Res.string.feature_safety_reason_lookalike_domain_explanation
        MessageSafetyWarningReason.MIXED_SCRIPT_DOMAIN -> Res.string.feature_safety_reason_mixed_script_domain_explanation
        MessageSafetyWarningReason.IP_ADDRESS_LINK -> Res.string.feature_safety_reason_ip_address_link_explanation
        MessageSafetyWarningReason.URL_SHORTENER -> Res.string.feature_safety_reason_url_shortener_explanation
        MessageSafetyWarningReason.URGENT_ACTION_REQUEST -> Res.string.feature_safety_reason_urgent_action_request_explanation
        MessageSafetyWarningReason.CREDENTIAL_REQUEST -> Res.string.feature_safety_reason_credential_request_explanation
        MessageSafetyWarningReason.PAYMENT_REQUEST -> Res.string.feature_safety_reason_payment_request_explanation
        MessageSafetyWarningReason.PRIVATE_KEY_REQUEST -> Res.string.feature_safety_reason_private_key_request_explanation
    }

@Preview
@Composable
private fun MessageSafetyDetailsScreenPreview() {
    SparrowTheme {
        MessageSafetyDetailsScreen(
            level = MessageSafetyWarningLevel.HIGH,
            reasons =
                listOf(
                    MessageSafetyWarningReason.CREDENTIAL_REQUEST,
                    MessageSafetyWarningReason.URGENT_ACTION_REQUEST
                ),
            focusReason = MessageSafetyWarningReason.CREDENTIAL_REQUEST,
            canBlockUser = true,
            isUserBlocked = false,
            isBlockingUser = false,
            blockError = null,
            onBackClick = {},
            onBlockUserClick = {}
        )
    }
}
