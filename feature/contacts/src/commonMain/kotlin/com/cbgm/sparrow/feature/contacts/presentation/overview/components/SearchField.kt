package com.cbgm.sparrow.feature.contacts.presentation.overview.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_search_placholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = MaterialTheme.spacing.small,
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium
                ).height(Dimens.ContactsScreen.searchHeight),
        textStyle =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onPrimary
            ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = searchQuery,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding =
                    PaddingValues(
                        horizontal = MaterialTheme.spacing.contactsScreen.searchHorizontalPadding,
                        vertical = MaterialTheme.spacing.contactsScreen.searchVerticalPadding
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.ContactsScreen.searchIconSize)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChanged("")
                            },
                            modifier = Modifier.size(Dimens.ContactsScreen.searchClearButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.ContactsScreen.searchIconSize)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text =
                            stringResource(
                                Res.string.feature_contacts_search_placholder
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.extraSmall,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor =
                            MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.primaryContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = .73f)
                    )
            )
        }
    )
}

@Preview
@Composable
fun SearchFieldPreview() {
    SparrowTheme {
        SearchField(
            searchQuery = "test",
            onSearchQueryChanged = {}
        )
    }
}
