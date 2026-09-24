package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.ListingRow
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_continue_action
import com.cbgm.sparrow.resources.feature_onboarding_contacts_stay_local
import com.cbgm.sparrow.resources.feature_onboarding_contacts_stay_local_description
import com.cbgm.sparrow.resources.feature_onboarding_e2ee_description
import com.cbgm.sparrow.resources.feature_onboarding_end_to_end_encryption
import com.cbgm.sparrow.resources.feature_onboarding_identity_belongs_to_you
import com.cbgm.sparrow.resources.feature_onboarding_identity_belongs_to_you_description
import com.cbgm.sparrow.resources.feature_onboarding_privacy_first
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.feature_onboarding_privacy_first),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
        )
        ListingRow(
            index = "01",
            title = stringResource(Res.string.feature_onboarding_end_to_end_encryption),
            description = stringResource(Res.string.feature_onboarding_e2ee_description)
        )
        ListingRow(
            index = "02",
            title = stringResource(Res.string.feature_onboarding_contacts_stay_local),
            description = stringResource(Res.string.feature_onboarding_contacts_stay_local_description)
        )
        ListingRow(
            index = "03",
            title = stringResource(Res.string.feature_onboarding_identity_belongs_to_you),
            description = stringResource(Res.string.feature_onboarding_identity_belongs_to_you_description)
        )
        SparrowApprovalButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small),
            text = stringResource(Res.string.base_continue_action)
        )
    }
}

@Preview
@Composable
private fun PrivacyPagePreview() {
    SparrowTheme { PrivacyPage {} }
}
