package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.feature_contacts_could_not_load_contact
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactDetailsErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_could_not_load_contact),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            SparrowApprovalButton(
                onClick = onRetry,
                text = stringResource(Res.string.base_retry)
            )
        }
    }
}

@Preview
@Composable
private fun ContactDetailsErrorContentPreview() {
    SparrowTheme {
        ContactDetailsErrorContent(
            message = "Contact could not be loaded",
            onRetry = {},
            modifier = Modifier.padding(MaterialTheme.spacing.screenPadding)
        )
    }
}
