package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsTopBar(
    title: String,
    containerColor: Color,
    onBack: () -> Unit
) {
    SparrowTopBar(
        containerColor = containerColor,
        title = {
            Text(
                text = title
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
        }
    )
}

@Preview
@Composable
private fun ContactDetailsTopBarPreview() {
    SparrowTheme {
        ContactDetailsTopBar(
            title = "Alex",
            containerColor = MaterialTheme.colorScheme.background,
            onBack = {}
        )
    }
}
