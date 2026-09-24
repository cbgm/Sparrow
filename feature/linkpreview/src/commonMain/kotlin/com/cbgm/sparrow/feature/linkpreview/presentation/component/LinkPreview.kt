package com.cbgm.sparrow.feature.linkpreview.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowImage
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.linkpreview.presentation.LinkPreviewViewModel
import com.cbgm.sparrow.feature.linkpreview.presentation.model.LinkPreviewUi
import com.cbgm.sparrow.feature.linkpreview.presentation.model.LinkPreviewUiState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LinkPreview(
    url: String,
    modifier: Modifier = Modifier
) {
    val viewModel =
        koinViewModel<LinkPreviewViewModel>(key = "link-preview:$url") {
            parametersOf(url)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LinkPreviewContent(
        url = url,
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
private fun LinkPreviewContent(
    url: String,
    uiState: LinkPreviewUiState,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        when (uiState) {
            LinkPreviewUiState.Loading ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

            is LinkPreviewUiState.Success ->
                LinkPreviewCard(
                    preview = uiState.preview,
                    onClick = { uriHandler.openUri(url) }
                )

            is LinkPreviewUiState.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = url,
                        modifier = Modifier.padding(MaterialTheme.spacing.base),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkPreviewCard(
    preview: LinkPreviewUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBytes = preview.imageBytes ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            SparrowImage(
                model = imageBytes,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PREVIEW_IMAGE_HEIGHT)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
                memoryCacheKey = "link-preview:${preview.url}"
            )

            preview.siteName?.takeIf(String::isNotBlank)?.let { site ->
                Text(
                    text = site,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            preview.title?.takeIf(String::isNotBlank)?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            preview.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val PREVIEW_IMAGE_HEIGHT = 140.dp

@Preview
@Composable
private fun LinkPreviewContentPreview() {
    SparrowTheme {
        LinkPreviewContent(
            url = "https://example.com/article",
            uiState =
                LinkPreviewUiState.Success(
                    LinkPreviewUi(
                        url = "https://example.com/article",
                        title = "Example article",
                        description = "A short description of the linked page.",
                        siteName = "Example",
                        imageBytes = byteArrayOf()
                    )
                )
        )
    }
}
