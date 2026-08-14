package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_sparrow
import com.cbgm.sparrow.resources.feature_contacts_sparrow_keys_attach_later
import com.cbgm.sparrow.resources.feature_contacts_sparrow_not_enabled
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NoSparrowIdentityContent() {
    SectionTitle(
        icon = Icons.Default.Security,
        title = stringResource(Res.string.base_sparrow)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    Text(
        text = stringResource(Res.string.feature_contacts_sparrow_not_enabled),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(Res.string.feature_contacts_sparrow_keys_attach_later),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Preview
@Composable
private fun NoSparrowIdentityContentPreview() {
    SparrowTheme {
        NoSparrowIdentityContent()
    }
}
