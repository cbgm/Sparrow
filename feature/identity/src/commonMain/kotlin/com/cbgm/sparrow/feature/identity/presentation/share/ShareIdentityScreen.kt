package com.cbgm.sparrow.feature.identity.presentation.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.BlockScreenshotEffect
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.platform.QrCode
import com.cbgm.sparrow.feature.identity.presentation.share.model.ShareIdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.share.model.ShareIdentityUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_create_qr_code
import com.cbgm.sparrow.resources.feature_identity_create_qr_description
import com.cbgm.sparrow.resources.feature_identity_hide_raw_identity
import com.cbgm.sparrow.resources.feature_identity_my_identity_qr
import com.cbgm.sparrow.resources.feature_identity_public_keys_always_included
import com.cbgm.sparrow.resources.feature_identity_raw_identity
import com.cbgm.sparrow.resources.feature_identity_scan_to_add_you
import com.cbgm.sparrow.resources.feature_identity_share_identity_text
import com.cbgm.sparrow.resources.feature_identity_share_identity_text_warning
import com.cbgm.sparrow.resources.feature_identity_show_raw_identity
import org.jetbrains.compose.resources.stringResource

/** Public identity-sharing screen contract. */
@Composable
fun ShareIdentityScreen(
    uiState: ShareIdentityUiState,
    onUiEvent: (ShareIdentityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    BlockScreenshotEffect(enabled = true)

    var showOverflowMenu by remember {
        mutableStateOf(false)
    }

    var showRawIdentity by remember {
        mutableStateOf(false)
    }

    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            ShareIdentityTopBar(
                uiState = uiState,
                containerColor = containerColor,
                showBackButton = showBackButton,
                showOverflowMenu = showOverflowMenu,
                showRawIdentity = showRawIdentity,
                onBack = { onUiEvent(ShareIdentityUiEvent.BackClicked) },
                onShowOverflowMenuChange = {
                    showOverflowMenu = it
                },
                onShowRawIdentityChange = {
                    showRawIdentity = it
                }
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                        start = MaterialTheme.spacing.screenPadding,
                        end = MaterialTheme.spacing.screenPadding
                    ).padding(vertical = MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.encodedIdentity.isNullOrBlank()) {
                IdentityOptionsContent(
                    uiState = uiState,
                    onGenerateClick = { onUiEvent(ShareIdentityUiEvent.GenerateClicked) }
                )
            } else {
                GeneratedIdentityContent(
                    encodedIdentity = uiState.encodedIdentity,
                    showRawIdentity = showRawIdentity,
                    onShareIdentity = { onUiEvent(ShareIdentityUiEvent.ShareClicked) }
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareIdentityTopBar(
    uiState: ShareIdentityUiState,
    containerColor: Color,
    showBackButton: Boolean,
    showOverflowMenu: Boolean,
    showRawIdentity: Boolean,
    onBack: () -> Unit,
    onShowOverflowMenuChange: (Boolean) -> Unit,
    onShowRawIdentityChange: (Boolean) -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_identity_my_identity_qr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (!uiState.encodedIdentity.isNullOrBlank()) {
                Box {
                    IconButton(
                        onClick = {
                            onShowOverflowMenuChange(true)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = {
                            onShowOverflowMenuChange(false)
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        if (showRawIdentity) {
                                            stringResource(Res.string.feature_identity_hide_raw_identity)
                                        } else {
                                            stringResource(Res.string.feature_identity_show_raw_identity)
                                        },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        if (showRawIdentity) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                onShowOverflowMenuChange(false)
                                onShowRawIdentityChange(
                                    !showRawIdentity
                                )
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun IdentityOptionsContent(
    uiState: ShareIdentityUiState,
    onGenerateClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Box(
            modifier =
                Modifier
                    .size(Dimens.ShareIdentityScreen.avatarSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.ShareIdentityScreen.codeBackground),
                        shape = MaterialTheme.shapes.circle
                    ).border(
                        width = Dimens.ShareIdentityScreen.dividerWidth,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.ShareIdentityScreen.codeBorder),
                        shape = MaterialTheme.shapes.circle
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.ShareIdentityScreen.actionIconContainerSize)
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_create_qr_description),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = stringResource(Res.string.feature_identity_public_keys_always_included),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        HorizontalDivider(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SparrowApprovalButton(
            onClick = onGenerateClick,
            enabled = !uiState.isGenerating,
            text =
                if (uiState.isGenerating) {
                    ""
                } else {
                    stringResource(Res.string.feature_identity_create_qr_code)
                },
            content = {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.ShareIdentityScreen.progressSize),
                        strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )
    }
}

@Composable
private fun GeneratedIdentityContent(
    encodedIdentity: String,
    showRawIdentity: Boolean,
    onShareIdentity: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = stringResource(Res.string.feature_identity_my_identity_qr),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = stringResource(Res.string.feature_identity_scan_to_add_you),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SparrowCard {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small
                            ).border(
                                width = Dimens.ShareIdentityScreen.dividerWidth,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.ShareIdentityScreen.accentText),
                                shape = MaterialTheme.shapes.small
                            ).padding(MaterialTheme.spacing.small)
                ) {
                    QrCode(
                        content = encodedIdentity,
                        modifier = Modifier.size(Dimens.ShareIdentityScreen.qrCodeSize)
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Button(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                    Text(
                        text = stringResource(Res.string.feature_identity_share_identity_text),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Text(
                    text = stringResource(Res.string.feature_identity_share_identity_text_warning),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = showRawIdentity,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = stringResource(Res.string.feature_identity_raw_identity),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                SparrowCardNoAnimation {
                    Text(
                        modifier = Modifier.padding(MaterialTheme.spacing.small),
                        text = encodedIdentity,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShareIdentityScreenPreview() {
    SparrowTheme {
        ShareIdentityScreen(
            uiState = ShareIdentityUiState(encodedIdentity = "test identity"),
            onUiEvent = {}
        )
    }
}
