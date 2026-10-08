package com.cbgm.sparrow.feature.expenses.presentation.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.messagepart.ui.model.ExpenseBoardUi
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_board
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseBoardMessageContent(
    board: ExpenseBoardUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Text(
            text = stringResource(Res.string.feature_expenses_board),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = board.currencyCode,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun ExpenseBoardMessageContentPreview() {
    SparrowTheme {
        ExpenseBoardMessageContent(
            board = ExpenseBoardUi(
                id = "expense-board-preview",
                currencyCode = "EUR",
                activatedAtEpochMilliseconds = 1L
            )
        )
    }
}
