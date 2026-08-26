package com.cbgm.sparrow.feature.media.data.datasource

import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryData

interface FileBrowserDataSource {
    fun hasFileAccess(): Boolean

    suspend fun setRootDirectory(reference: String): Result<Unit>

    suspend fun getRootDirectory(): Result<FileBrowserDirectoryData>

    suspend fun listDirectory(reference: String): Result<List<FileBrowserEntryData>>

    suspend fun readFile(reference: String, maxByteSize: Long): Result<FileBrowserContentData>
}
