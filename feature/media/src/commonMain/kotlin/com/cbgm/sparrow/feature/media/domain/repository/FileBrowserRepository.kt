package com.cbgm.sparrow.feature.media.domain.repository

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry

interface FileBrowserRepository {
    fun hasFileAccess(): Boolean

    suspend fun setRootDirectory(reference: String): Result<Unit>

    suspend fun getRootDirectory(): Result<FileBrowserDirectory>

    suspend fun listDirectory(reference: String): Result<List<FileBrowserEntry>>

    suspend fun readFile(reference: String, maxByteSize: Long): Result<FileBrowserContent>
}
