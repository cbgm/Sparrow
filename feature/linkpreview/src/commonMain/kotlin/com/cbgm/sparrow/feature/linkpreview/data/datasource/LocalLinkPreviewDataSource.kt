package com.cbgm.sparrow.feature.linkpreview.data.datasource

import com.cbgm.sparrow.data.database.dao.LinkPreviewDao
import com.cbgm.sparrow.data.database.entity.LinkPreviewEntity
import com.cbgm.sparrow.feature.linkpreview.data.mapper.toEntity
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDto

class LocalLinkPreviewDataSource(
    private val linkPreviewDao: LinkPreviewDao
) {
    suspend fun getPreview(url: String): LinkPreviewEntity? =
        linkPreviewDao.getByUrl(url)

    suspend fun insertPreview(linkPreview: LinkPreviewDto) {
        linkPreviewDao.upsert(linkPreview.toEntity())
    }
}
