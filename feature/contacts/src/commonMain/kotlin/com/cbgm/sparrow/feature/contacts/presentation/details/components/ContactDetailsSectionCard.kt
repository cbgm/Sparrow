package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
internal fun ContactDetailsSectionCard(content: @Composable () -> Unit) {
    SparrowCardNoAnimation {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            content()
        }
    }
}

@Preview
@Composable
private fun ContactDetailsSectionCardPreview() {
    SparrowTheme {
        ContactDetailsSectionCard {
            Text(text = "Contact details")
        }
    }
}
