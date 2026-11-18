package com.cbgm.sparrow.feature.chats.presentation.common.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.HeaderTitle
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderUiModel
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import org.jetbrains.compose.resources.stringResource

/** Shared header chrome. Conversation-specific status or security content is supplied as a slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderContent(
    model: HeaderUiModel,
    containerColor: Color,
    onBackClick: () -> Unit,
    onHeaderClick: () -> Unit,
    actions: @Composable () -> Unit = {},
    belowHeader: @Composable () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SparrowTopBar(
            containerColor = containerColor,
            title = { HeaderTitle(model = model, onClick = onHeaderClick) },
            actions = { actions() },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = SparrowIcons.ArrowBack,
                        contentDescription = stringResource(Res.string.base_back),
                        modifier = Modifier.size(Dimens.TopBar.iconSize)
                    )
                }
            }
        )
        belowHeader()
    }
}
