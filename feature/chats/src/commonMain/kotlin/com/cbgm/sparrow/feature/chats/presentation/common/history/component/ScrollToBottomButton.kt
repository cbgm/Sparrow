package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.Alpha
import kotlinx.coroutines.launch

@Composable
internal fun ScrollToBottomButton(
    listState: LazyListState,
    reverseLayout: Boolean,
    modifier: Modifier = Modifier,
    visibilityThresholdItems: Int = SCROLL_TO_BOTTOM_VISIBILITY_THRESHOLD_ITEMS
) {
    val coroutineScope = rememberCoroutineScope()
    val isVisible by remember(listState, reverseLayout, visibilityThresholdItems) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || layoutInfo.totalItemsCount <= 1) {
                return@derivedStateOf false
            }

            if (reverseLayout) {
                visibleItems.minOf { item -> item.index } >= visibilityThresholdItems
            } else {
                val itemsBelowViewport =
                    layoutInfo.totalItemsCount - 1 - visibleItems.maxOf { item -> item.index }
                itemsBelowViewport >= visibilityThresholdItems
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        SmallFloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    val targetIndex =
                        if (reverseLayout) {
                            0
                        } else {
                            listState.layoutInfo.totalItemsCount - 1
                        }

                    if (targetIndex >= 0) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = Alpha.FloatingButton.opaque),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
}

private const val SCROLL_TO_BOTTOM_VISIBILITY_THRESHOLD_ITEMS = 3
