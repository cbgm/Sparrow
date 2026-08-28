package com.cbgm.sparrow.feature.contacts.presentation.overview.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_search_placholder
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSelectionContactsTopBar(
    title: String,
    searchQuery: String,
    confirmEnabled: Boolean,
    confirming: Boolean,
    containerColor: Color,
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onConfirmed: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
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
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
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
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)
        )
    }
}

@Preview
@Composable
private fun MemberSelectionContactsTopBarPreview() {
    SparrowTheme {
        MemberSelectionContactsTopBar(
            title = "Add members",
            searchQuery = "",
            confirmEnabled = true,
            confirming = false,
            containerColor = MaterialTheme.colorScheme.background,
            onBack = {},
            onSearchQueryChanged = {},
            onConfirmed = {}
        )
    }
}
