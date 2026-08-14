package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.scroll.BarsState
import com.cbgm.sparrow.core.ui.scroll.rememberBarsState
import com.cbgm.sparrow.core.ui.theme.Alpha

@Composable
fun SparrowLazyScaffold(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    background: @Composable BoxScope.() -> Unit = {},
    topBar: @Composable (containerColor: Color) -> Unit = {},
    bottomBar: @Composable (containerColor: Color) -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        listState: LazyListState
    ) -> Unit
) {
    val listState = rememberLazyListState()

    val barsState =
        rememberBarsState(
            state = listState,
            fadedAlpha = fadedAlpha
        )

    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha),
        label = "SparrowTopBarColor"
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha),
        label = "SparrowBottomBarColor"
    )

    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            },
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            snackbarHost = {
                snackbarHostState?.let { state ->
                    SnackbarHost(hostState = state)
                }
            }
        ) { innerPadding ->
            content(
                innerPadding,
                listState
            )
        }
    }
}

@Composable
fun SparrowScrollScaffold(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    background: @Composable () -> Unit = {},
    topBar: @Composable (Color) -> Unit = {},
    bottomBar: @Composable (Color) -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        scrollState: ScrollState
    ) -> Unit
) {
    val scrollState = rememberScrollState()

    val barsState =
        rememberBarsState(
            state = scrollState,
            fadedAlpha = fadedAlpha
        )

    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha),
        label = "SparrowTopBarColor"
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha),
        label = "SparrowBottomBarColor"
    )

    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            }
        ) { innerPadding ->
            content(
                innerPadding,
                scrollState
            )
        }
    }
}

@Composable
fun SparrowStaticScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    background: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content
        )
    }
}

@Stable
data class SparrowMainScrollStates(
    val chats: LazyListState,
    val identity: ScrollState,
    val settings: ScrollState
)

enum class SparrowMainScrollTarget {
    Chats,
    Identity,
    Settings
}

@Stable
sealed interface SparrowScrollState {
    @Stable
    data class LazyList(
        val state: LazyListState
    ) : SparrowScrollState

    @Stable
    data class Scroll(
        val state: ScrollState
    ) : SparrowScrollState
}

@Stable
class SparrowTabbedScrollStates<T : Any> internal constructor(
    private val states: Map<T, SparrowScrollState>
) {
    operator fun get(
        target: T
    ): SparrowScrollState =
        requireNotNull(states[target]) {
            "No scroll state registered for target: $target"
        }

    fun lazyListState(
        target: T
    ): LazyListState =
        when (val scrollState = get(target)) {
            is SparrowScrollState.LazyList -> {
                scrollState.state
            }

            is SparrowScrollState.Scroll -> {
                error(
                    "Target $target uses ScrollState, not LazyListState."
                )
            }
        }

    fun scrollState(
        target: T
    ): ScrollState =
        when (val scrollState = get(target)) {
            is SparrowScrollState.Scroll -> {
                scrollState.state
            }

            is SparrowScrollState.LazyList -> {
                error(
                    "Target $target uses LazyListState, not ScrollState."
                )
            }
        }
}

enum class SparrowScrollStateType {
    LazyList,
    Scroll
}

@Composable
fun <T : Any> rememberSparrowTabbedScrollStates(
    targets: Map<T, SparrowScrollStateType>
): SparrowTabbedScrollStates<T> {
    require(targets.isNotEmpty()) {
        "At least one scroll target is required."
    }

    require(targets.size <= 5) {
        "SparrowTabbedScaffold supports at most 5 scroll targets."
    }

    val rememberedStates =
        targets.mapValues { (_, type) ->
            when (type) {
                SparrowScrollStateType.LazyList -> {
                    SparrowScrollState.LazyList(
                        state = rememberLazyListState()
                    )
                }

                SparrowScrollStateType.Scroll -> {
                    SparrowScrollState.Scroll(
                        state = rememberScrollState()
                    )
                }
            }
        }

    return remember(rememberedStates) {
        SparrowTabbedScrollStates(
            states = rememberedStates
        )
    }
}

@Composable
fun <T : Any> SparrowTabbedScaffold(
    selectedScrollTarget: T,
    scrollTargets: Map<T, SparrowScrollStateType>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.background,
    fadedAlpha: Float = Alpha.OpaqueBar,
    containerColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    background: @Composable BoxScope.() -> Unit = {},
    topBar: @Composable (containerColor: Color) -> Unit = {},
    bottomBar: @Composable (containerColor: Color) -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (
        innerPadding: PaddingValues,
        scrollStates: SparrowTabbedScrollStates<T>
    ) -> Unit
) {
    require(selectedScrollTarget in scrollTargets) {
        "Selected target $selectedScrollTarget is not registered."
    }

    val scrollStates =
        rememberSparrowTabbedScrollStates(
            targets = scrollTargets
        )

    val barsState =
        when (
            val selectedState =
                scrollStates[selectedScrollTarget]
        ) {
            is SparrowScrollState.LazyList -> {
                rememberBarsState(
                    state = selectedState.state,
                    fadedAlpha = fadedAlpha
                )
            }

            is SparrowScrollState.Scroll -> {
                rememberBarsState(
                    state = selectedState.state,
                    fadedAlpha = fadedAlpha
                )
            }
        }

    SparrowTabbedScaffoldContent(
        barsState = barsState,
        barColor = barColor,
        containerColor = containerColor,
        modifier = modifier,
        floatingActionButtonPosition =
        floatingActionButtonPosition,
        background = background,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton =
        floatingActionButton
    ) { innerPadding ->
        content(
            innerPadding,
            scrollStates
        )
    }
}

@Composable
private fun SparrowTabbedScaffoldContent(
    barsState: BarsState,
    barColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    floatingActionButtonPosition: FabPosition,
    background: @Composable BoxScope.() -> Unit,
    topBar: @Composable (containerColor: Color) -> Unit,
    bottomBar: @Composable (containerColor: Color) -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val topBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.topBarAlpha)
    )

    val bottomBarColor by animateColorAsState(
        targetValue = barColor.copy(alpha = barsState.bottomBarAlpha)
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        background()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = containerColor,
            topBar = {
                topBar(topBarColor)
            },
            bottomBar = {
                bottomBar(bottomBarColor)
            },
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content
        )
    }
}
