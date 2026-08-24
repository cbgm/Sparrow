package com.cbgm.sparrow.feature.media.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.rectangle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType
import com.cbgm.sparrow.feature.media.presentation.platform.PlatformMediaImage
import com.cbgm.sparrow.feature.media.presentation.platform.PlatformVideoPlayer

/**
 * Reusable full-screen media viewer. A one-item list is the single-photo case.
 */
@Composable
fun MediaViewer(
    media: List<MediaItem>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onEnsureMediaLoaded: (String) -> Unit = {},
    title: (currentIndex: Int, total: Int) -> String = { currentIndex, total ->
        "${currentIndex + 1}/$total"
    },
    topBarActions: @Composable RowScope.() -> Unit = {}
) {
    if (media.isEmpty()) return

    val safeInitialIndex = initialIndex.coerceIn(0, media.lastIndex)

    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = media::size
    )

    LaunchedEffect(initialIndex, media.size) {
        val requestedIndex = initialIndex.coerceIn(0, media.lastIndex)
        if (requestedIndex != pagerState.currentPage) {
            pagerState.scrollToPage(requestedIndex)
        }
    }

    SparrowOverlayHost(
        visible = true,
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxSize(),
        horizontalPadding = MaterialTheme.spacing.zero,
        topPadding = MaterialTheme.spacing.zero,
        shape = MaterialTheme.shapes.rectangle
    ) { dismissOverlay ->
        SparrowStaticScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = FunctionalColors.MediaBackground,
            topBar = {
                MediaViewerTopBar(
                    title = title(pagerState.currentPage, media.size),
                    onBack = dismissOverlay,
                    actions = topBarActions
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                key = { page -> media[page].id }
            ) { page ->
                MediaViewerPage(
                    media = media[page],
                    isActive = page == pagerState.currentPage,
                    onEnsureMediaLoaded = onEnsureMediaLoaded,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaViewerTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = actions
    )
}

@Composable
private fun MediaViewerPage(
    media: MediaItem,
    isActive: Boolean,
    onEnsureMediaLoaded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(media.id, media.bytes) {
        if (media.bytes == null) {
            onEnsureMediaLoaded(media.id)
        }
    }

    if (media.bytes == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(FunctionalColors.MediaBackground)
        )
        return
    }

    when (media.type) {
        MediaType.IMAGE ->
            PlatformMediaImage(
                data = media.bytes,
                cacheKey = "media-full:${media.id}",
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

        MediaType.VIDEO ->
            MediaVideoPage(
                media = media,
                isActive = isActive,
                modifier = modifier
            )
    }
}

@Composable
private fun MediaVideoPage(
    media: MediaItem,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var playRequested by remember(media.id) { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) playRequested = false
    }

    if (isActive && playRequested) {
        PlatformVideoPlayer(
            media = media,
            isActive = true,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = isActive) { playRequested = true },
        contentAlignment = Alignment.Center
    ) {
        MediaThumbnail(
            media = media,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(Dimens.Media.playIconSize)
        )
    }
}

@Preview
@Composable
private fun MediaViewerPreview() {
    SparrowTheme {
        MediaViewer(
            media =
                listOf(
                    MediaItem(
                        id = "preview-image",
                        type = MediaType.IMAGE,
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf()
                    )
                ),
            initialIndex = 0,
            onDismiss = {},
            onEnsureMediaLoaded = {}
        )
    }
}
