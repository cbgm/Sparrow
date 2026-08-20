package com.cbgm.sparrow.core.ui.component

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun SparrowSearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    focusRequester: FocusRequester? = null
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
                ).height(Dimens.SearchField.searchHeight)
                .focusRequester(focusRequester ?: FocusRequester.Cancel),
        textStyle =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
                        horizontal = MaterialTheme.spacing.searchField.searchHorizontalPadding,
                        vertical = MaterialTheme.spacing.searchField.searchVerticalPadding
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onClear()
                            },
                            modifier = Modifier.size(Dimens.SearchField.searchClearButtonSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.SearchField.searchIconSize)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.extraSmall,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
        }
    )
}

@Preview
@Composable
fun SearchFieldPreview() {
    SparrowTheme {
        SparrowSearchField(
            searchQuery = "test",
            onSearchQueryChanged = {},
            placeholder = "Test",
            onClear = {}
        )
    }
}
