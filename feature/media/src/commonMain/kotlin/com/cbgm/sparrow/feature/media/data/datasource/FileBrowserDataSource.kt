package com.cbgm.sparrow.feature.media.data.datasource

import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryDto

interface FileBrowserDataSource {
    fun hasFileAccess(): Boolean

    suspend fun setRootDirectory(reference: String)

    suspend fun getRootDirectory(): FileBrowserDirectoryDto

    suspend fun listDirectory(reference: String): List<FileBrowserEntryDto>

    suspend fun readFile(reference: String, maxByteSize: Long): FileBrowserContentDto
}
