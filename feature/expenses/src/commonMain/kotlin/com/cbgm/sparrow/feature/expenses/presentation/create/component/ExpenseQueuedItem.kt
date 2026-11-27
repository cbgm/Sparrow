package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseDraftUi
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_paid_by_participants
import com.cbgm.sparrow.resources.feature_expenses_remove
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpenseQueuedItem(
    draft: ExpenseDraftUi,
    number: Int,
    participants: List<ExpenseParticipantUi>,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Icon(
                draft.category.icon,
                contentDescription = draft.category.label(),
                modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                tint = MaterialTheme.colorScheme.outline
            )
            val payer = participants.firstOrNull { it.id == draft.payerId }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = draft.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        Res.string.feature_expenses_paid_by_participants,
                        payer?.displayName.orEmpty(),
                        draft.participantIds.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = draft.formattedAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = SparrowIcons.Delete,
                        contentDescription = stringResource(Res.string.feature_expenses_remove),
                        modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        HorizontalDivider(thickness = Dimens.Base.dividerThickness, color = MaterialTheme.colorScheme.divider)
    }
}

@Preview
@Composable
private fun ExpenseQueuedItemPreview() {
    SparrowTheme {
        ExpenseQueuedItem(
            draft = ExpenseDraftUi(
                "Taxi",
                "15.00",
                "a",
                setOf("a", "b"),
                ExpenseCategoryUi.TRANSPORT
            ),
            number = 1,
            participants = listOf(
                ExpenseParticipantUi("a", "Chris", true),
                ExpenseParticipantUi("b", "Alex")
            ),
            enabled = true,
            onDelete = {}
        )
    }
}
