package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.modal
import com.cbgm.sparrow.core.ui.theme.rectangle
import com.cbgm.sparrow.core.ui.theme.spacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparrowOverlayHost(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MaterialTheme.spacing.small,
    topPadding: Dp = MaterialTheme.spacing.times(6),
    shape: Shape = MaterialTheme.shapes.modal,
    // containerColor: Color = MaterialTheme.colorScheme.background,
    scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = Alpha.Disabled),
    // tonalElevation: Dp = 8.dp,
    // shadowElevation: Dp = 12.dp,
    content: @Composable (
        dismissOverlay: () -> Unit
    ) -> Unit
) {
    val scope = rememberCoroutineScope()

    val currentOnDismissRequest by
        rememberUpdatedState(onDismissRequest)

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    var mounted by remember {
        mutableStateOf(false)
    }

    var transitionRunning by remember {
        mutableStateOf(false)
    }

    fun dismissOverlay() {
        if (transitionRunning) {
            return
        }

        transitionRunning = true

        scope.launch {
            try {
                if (sheetState.isVisible) {
                    sheetState.hide()
                }

                mounted = false
                currentOnDismissRequest()
            } finally {
                transitionRunning = false
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            if (mounted || transitionRunning) {
                return@LaunchedEffect
            }

            transitionRunning = true
            mounted = true

            try {
                // Let the sheet enter composition first.
                yield()

                sheetState.show()
            } finally {
                transitionRunning = false
            }
        } else if (mounted && !transitionRunning) {
            transitionRunning = true

            try {
                if (sheetState.isVisible) {
                    sheetState.hide()
                }

                mounted = false
            } finally {
                transitionRunning = false
            }
        }
    }

    if (!mounted) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = ::dismissOverlay,
        modifier = modifier.fillMaxSize(),
        sheetState = sheetState,
        sheetGesturesEnabled = !transitionRunning,
        shape = MaterialTheme.shapes.rectangle,
        containerColor = Color.Transparent,
        tonalElevation = Dimens.Base.zero,
        scrimColor = scrimColor,
        dragHandle = null,
        contentWindowInsets = {
            WindowInsets(
                left = 0,
                top = 0,
                right = 0,
                bottom = 0
            )
        }
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding
                    )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = shape
                // color = containerColor,
                // tonalElevation = tonalElevation,
                // shadowElevation = shadowElevation
            ) {
                content(::dismissOverlay)
            }
        }
    }
}

@Preview
@Composable
fun OverlayHostPreview() {
    SparrowTheme {
        SparrowOverlayHost(
            visible = true,
            onDismissRequest = {},
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = MaterialTheme.spacing.zero,
            topPadding = MaterialTheme.spacing.times(6),
            // tonalElevation = 8.dp,
            // shadowElevation = 12.dp,
            content = {
            }
        )
    }
}
