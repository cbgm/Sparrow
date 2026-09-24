package com.cbgm.sparrow.feature.linkpreview.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.linkpreview.data.datasource.LocalLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.datasource.RemoteLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.mapper.toDomain
import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkPreviewRepositoryImpl(
    private val remoteLinkPreviewDataSource: RemoteLinkPreviewDataSource,
    private val localLinkPreviewDataSource: LocalLinkPreviewDataSource,
    private val applicationScope: ApplicationCoroutineScope
) : LinkPreviewRepository {
    // No Result returned because the packet listener doesn't wait around.
    override fun fetchPreviews(text: String) {
        applicationScope.launch(Dispatchers.Default) {
            try {
                val urls = URL_REGEX
                    .findAll(text)
                    .map { match -> match.value.trimEnd(*TRAILING_LINK_PUNCTUATION) }
                    .filter(String::isNotBlank)
                    .distinct()
                    .toList()

                if (urls.isEmpty()) return@launch

                urls.forEach { url ->
                    getPreview(url)
                }
            } catch (error: Exception) {
                val logger = SparrowLog.withTag("LinkPreviewPacketProcessor")
                logger.error(throwable = error, message = { "Failed to process message packet text" })
            }
        }
    }

    override suspend fun getPreview(url: String): Result<LinkPreview> =
        safeSuspendCall {
            // A page may legitimately have no preview image. Cache that result as well,
            // so opening a chat or prefetching its messages does not fetch it repeatedly.
            localLinkPreviewDataSource.getPreview(url)?.toDomain()
                ?: remoteLinkPreviewDataSource.getPreview(url)
                    .also { preview -> localLinkPreviewDataSource.insertPreview(preview) }
                    .toDomain()
        }
}

private val URL_REGEX = Regex("https?://[^\\s<>{}\\[\\]]+")
private val TRAILING_LINK_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', '\'', '"')
