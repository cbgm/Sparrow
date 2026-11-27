package com.cbgm.sparrow.feature.contacts.presentation.overview.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_name
import com.cbgm.sparrow.resources.feature_contacts_search_placholder
import com.cbgm.sparrow.resources.feature_onboarding_create
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionContactsTopBar(
    title: String,
    searchQuery: String,
    confirmEnabled: Boolean,
    confirming: Boolean,
    containerColor: Color,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onConfirmed: () -> Unit
) {
    Column {
        SparrowTopBar(
            windowInsets = WindowInsets(MaterialTheme.spacing.zero),
            containerColor = containerColor,
            title = {
                // The group name is typed right where a title sits, as before.
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChanged,
                    enabled = !confirming,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.feature_chats_group_name),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = SparrowIcons.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.TopBar.iconSize)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onConfirmed,
                    enabled = confirmEnabled && !confirming
                ) {
                    if (confirming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.ContactsScreen.selectionProgressSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Icon(
                            imageVector = SparrowIcons.Check,
                            contentDescription = stringResource(Res.string.feature_onboarding_create),
                            modifier = Modifier.size(Dimens.TopBar.iconSize)
                        )
                    }
                }
            }
        )

        SparrowSearchField(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            placeholder = stringResource(Res.string.feature_contacts_search_placholder),
            onClear = { onSearchQueryChanged("") },
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
                bottom = MaterialTheme.spacing.small
            )
        )
    }
}

@Preview
@Composable
private fun TopBarPreview() {
    SparrowTheme {
        GroupSelectionContactsTopBar(
            title = "Test",
            searchQuery = "",
            confirmEnabled = true,
            confirming = false,
            containerColor = MaterialTheme.colorScheme.background,
            onBack = {},
            onTitleChanged = {},
            onSearchQueryChanged = {},
            onConfirmed = {}
        )
    }
}
