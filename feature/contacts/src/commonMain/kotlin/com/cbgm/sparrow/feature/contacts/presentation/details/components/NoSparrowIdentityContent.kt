package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contacts_sparrow_keys_attach_later
import com.cbgm.sparrow.resources.feature_contacts_sparrow_not_enabled
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NoSparrowIdentityContent() {
    ContactStatusRow(
        dotColor = MaterialTheme.colorScheme.error,
        title = stringResource(Res.string.feature_contacts_sparrow_not_enabled),
        description = stringResource(Res.string.feature_contacts_sparrow_keys_attach_later)
    )
}

@Preview
@Composable
private fun NoSparrowIdentityContentPreview() {
    SparrowTheme { NoSparrowIdentityContent() }
}
