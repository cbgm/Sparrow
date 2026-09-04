package com.cbgm.sparrow.feature.chats.presentation.forwarding.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiEvent
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingTargetUi

internal fun LazyListScope.forwardingTargetSection(
    title: String,
    targets: List<ForwardingTargetUi>,
    onUiEvent: (ForwardingSelectionUiEvent) -> Unit
) {
    if (targets.isEmpty()) return

    item(key = "section:$title") {
        Text(
            text = title,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.screenPadding,
                        vertical = MaterialTheme.spacing.small
                    ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    items(
        items = targets,
        key = ForwardingTargetUi::id
    ) { target ->
        ForwardingTargetItem(
            target = target,
            onClick = {
                onUiEvent(ForwardingSelectionUiEvent.TargetClicked(target))
            }
        )
    }
}
