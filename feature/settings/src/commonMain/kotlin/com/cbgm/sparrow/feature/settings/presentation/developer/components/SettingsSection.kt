package com.cbgm.sparrow.feature.settings.presentation.developer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun SettingsSection(
    title: String,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        SparrowSectionLabel(
            text = title,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.micro),
            color = color
        )
        content()
    }
}
