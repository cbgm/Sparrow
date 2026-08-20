package com.cbgm.sparrow.feature.chats.presentation.direct.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.component.MessageBubble
import com.cbgm.sparrow.feature.chats.presentation.component.MessageInput
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.component.rememberMessageSearchTargetState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_chat_no_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_one_way_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_typing
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_description
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_title
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_e2ee
import com.cbgm.sparrow.resources.feature_chats_chat_verified_keys_description
import com.cbgm.sparrow.resources.feature_chats_import_contact_identity
import com.cbgm.sparrow.resources.feature_chats_loading_chat
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_action
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_title
import com.cbgm.sparrow.resources.feature_chats_start_conversation_with
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectScreen(
    uiState: DirectUiState,
    onUiEvent: (DirectUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null
) {
    var showIdentitySetupDialog by rememberSaveable { mutableStateOf(false) }

    SparrowLazyScaffold(
        modifier = modifier,
        barColor = MaterialTheme.colorScheme.background,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = Alpha.PatternBackground.conversation
            )
        },
        topBar = { containerColor ->
            TopBar(
                uiState = uiState,
                containerColor = containerColor,
                onUiEvent = onUiEvent,
                onManualIdentitySetup = { showIdentitySetupDialog = true }
            )
        },
        bottomBar = { containerColor ->
            BottomBar(
                uiState = uiState,
                containerColor = containerColor,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding, listState ->
        Content(
            uiState = uiState,
            listState = listState,
            innerPadding = innerPadding,
            targetMessageId = targetMessageId,
            onRetryMessage = { messageId ->
                onUiEvent(DirectUiEvent.RetryMessage(messageId))
            }
        )
    }

    if (showIdentitySetupDialog) {
        IdentitySetupDialog(
            onShareIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectUiEvent.ShareIdentityClicked)
            },
            onImportIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectUiEvent.ImportIdentityClicked)
            },
            onDismiss = { showIdentitySetupDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: DirectUiState,
    containerColor: Color,
    onUiEvent: (DirectUiEvent) -> Unit,
    onManualIdentitySetup: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Row(
                    modifier = Modifier.clickable { onUiEvent(DirectUiEvent.HeaderClicked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowAvatar(
                        name = uiState.contactName,
                        pictureBytes = uiState.profilePictureBytes,
                        size = Dimens.DirectScreen.topBarAvatarSize
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = uiState.contactName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { onUiEvent(DirectUiEvent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        if (!uiState.isLoading) {
            SecurityBanner(
                securityState = uiState.contactSecurityState,
                identitySetupMode = uiState.identitySetupMode,
                isChatAuthorized = uiState.isChatAuthorized,
                onVerifyIdentity = { onUiEvent(DirectUiEvent.VerifyIdentityClicked) },
                onManualIdentitySetup = onManualIdentitySetup
            )
        }

        uiState.errorMessage?.let { message -> ErrorMessage(message = message) }
    }
}

@Composable
private fun IdentitySetupDialog(
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_chats_manual_identity_setup_title),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                SparrowOutlinedButton(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_share_my_identity)
                )
                SparrowOutlinedButton(
                    onClick = onImportIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_chats_import_contact_identity)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowSecondaryButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun IdentitySetupDialogPreview() {
    SparrowTheme {
        IdentitySetupDialog(
            onShareIdentity = {},
            onImportIdentity = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun BottomBar(
    uiState: DirectUiState,
    containerColor: Color,
    onUiEvent: (DirectUiEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text =
                    if (uiState.isContactTyping) {
                        stringResource(
                            Res.string.feature_chats_chat_typing,
                            uiState.contactName
                        )
                    } else {
                        ""
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.large,
                            vertical = MaterialTheme.spacing.base / 2
                        ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MessageInput(
                value = uiState.messageText,
                onValueChange = { onUiEvent(DirectUiEvent.MessageTextChanged(it)) },
                onSendClick = { onUiEvent(DirectUiEvent.SendClicked) },
                inputEnabled = !uiState.isLoading && uiState.composerState.isInputEnabled,
                sendEnabled = !uiState.isLoading && uiState.composerState.isSendActionEnabled
            )
        }
    }
}

@Composable
private fun Content(
    uiState: DirectUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    targetMessageId: String?,
    onRetryMessage: (String) -> Unit
) {
    when {
        uiState.isLoading -> LoadingContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )

        uiState.messages.isEmpty() -> EmptyContent(
            contactName = uiState.contactName,
            securityState = uiState.contactSecurityState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )

        else -> MessageList(
            messages = uiState.messages,
            listState = listState,
            targetMessageId = targetMessageId,
            onRetryMessage = onRetryMessage,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun MessageList(
    messages: List<MessageBubbleModel>,
    listState: LazyListState,
    targetMessageId: String?,
    onRetryMessage: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val searchTargetState =
        rememberMessageSearchTargetState(
            targetMessageId = targetMessageId,
            messageIds = messages.map(MessageBubbleModel::id),
            listState = listState
        )
    val newestMessage = messages.firstOrNull()
    LaunchedEffect(newestMessage?.id) {
        if (searchTargetState.isHandled && newestMessage?.isMine == true) {
            listState.animateScrollToItem(index = 0)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.messageList.horizontalPadding,
                top = contentPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.messageList.horizontalPadding,
                bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.small
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        items(items = messages, key = MessageBubbleModel::id) { message ->
            MessageBubble(
                message = message,
                onRetryClick = { onRetryMessage(message.id) },
                isSearchHighlighted = message.id == searchTargetState.highlightedMessageId
            )
        }
    }
}

@Composable
private fun EmptyContent(
    contactName: String,
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    Res.string.feature_chats_start_conversation_with,
                    contactName
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = securityDescription(securityState),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun securityDescription(securityState: ContactSecurityState): String =
    when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> stringResource(Res.string.feature_chats_chat_no_keys_description)
        ContactSecurityState.ONE_WAY_KEYS -> stringResource(Res.string.feature_chats_chat_one_way_keys_description)
        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> stringResource(Res.string.feature_chats_chat_unverified_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME -> stringResource(Res.string.feature_chats_chat_verified_by_me_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> stringResource(Res.string.feature_chats_chat_verified_by_contact_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> stringResource(Res.string.feature_chats_chat_verified_keys_description)
    }

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(Res.string.feature_chats_loading_chat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SecurityBanner(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode,
    isChatAuthorized: Boolean,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED && isChatAuthorized) {
        VerifiedSecurityIndicator(modifier = modifier)
        return
    }

    val state =
        securityState(
            securityState = securityState,
            identitySetupMode = identitySetupMode
        ) ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = state.containerColor,
        contentColor = state.contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directScreen.securityBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.invitationIconSize)
            )
            Column(
                modifier = Modifier.padding(start = MaterialTheme.spacing.small).weight(1f)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.description,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.directScreen.securityDescriptionTopPadding),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            SecurityAction(
                securityState = securityState,
                identitySetupMode = identitySetupMode,
                contentColor = state.contentColor,
                onVerifyIdentity = onVerifyIdentity,
                onManualIdentitySetup = onManualIdentitySetup
            )
        }
    }
}

@Composable
private fun SecurityAction(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode,
    contentColor: Color,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit
) {
    when {
        identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING &&
            securityState in setOf(
                ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
                ContactSecurityState.ONE_WAY_KEYS
            ) -> {
            TextButton(onClick = onManualIdentitySetup) {
                Text(
                    text = stringResource(Res.string.feature_chats_manual_identity_setup_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ||
            securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> {
            TextButton(onClick = onVerifyIdentity) {
                Text(
                    text = stringResource(Res.string.base_verify),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun securityState(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode
): SecurityBannerState? =
    when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title =
                    if (identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                        stringResource(Res.string.feature_chats_manual_identity_required_title)
                    } else {
                        stringResource(Res.string.feature_chats_chat_unencrypted_title)
                    },
                description =
                    if (identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                        stringResource(Res.string.feature_chats_manual_identity_required_description)
                    } else {
                        stringResource(Res.string.feature_chats_chat_unencrypted_description)
                    }
            )

        ContactSecurityState.ONE_WAY_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title =
                    if (identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                        stringResource(Res.string.feature_chats_manual_identity_incomplete_title)
                    } else {
                        stringResource(Res.string.feature_chats_chat_key_exchange_incomplete_title)
                    },
                description =
                    if (identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
                        stringResource(Res.string.feature_chats_manual_identity_incomplete_description)
                    } else {
                        stringResource(Res.string.feature_chats_chat_key_exchange_incomplete_description)
                    }
            )

        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
            errorBanner(
                title = stringResource(Res.string.feature_chats_chat_unverified_title),
                description = stringResource(Res.string.feature_chats_chat_unverified_description)
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
            SecurityBannerState(
                icon = Icons.Default.Schedule,
                title = stringResource(Res.string.feature_chats_chat_verified_by_me_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_me_description),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
            SecurityBannerState(
                icon = Icons.Default.Security,
                title = stringResource(Res.string.feature_chats_chat_verified_by_contact_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_contact_description),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> null
    }

@Composable
private fun errorBanner(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Warning
) =
    SecurityBannerState(
        icon = icon,
        title = title,
        description = description,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )

@Composable
private fun VerifiedSecurityIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Alpha.DirectScreen.securityBanner),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directScreen.verifiedBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.statusIconSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.directScreen.verifiedContentGap))
            Text(
                text = stringResource(Res.string.feature_chats_chat_verified_e2ee),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private data class SecurityBannerState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)

@Preview
@Composable
private fun DirectScreenPreview() {
    SparrowTheme {
        DirectScreen(
            uiState =
                DirectUiState(
                    contactName = "Alex",
                    isLoading = false,
                    composerState = DirectComposerState.READY,
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun DirectMessagesPreview() {
    SparrowTheme {
        DirectScreen(
            uiState =
                DirectUiState(
                    contactName = "Alex",
                    isLoading = false,
                    composerState = DirectComposerState.READY,
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                    messages =
                        listOf(
                            MessageBubbleModel(
                                id = "1",
                                isMine = true,
                                text = "Hello from a direct chat",
                                security = MessageSecurity.END_TO_END_ENCRYPTED,
                                contentStatus = MessageContentStatus.READABLE,
                                deliveryStatus = MessageDeliveryStatus.DELIVERED
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
