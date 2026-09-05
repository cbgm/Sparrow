package com.cbgm.sparrow.feature.media.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.mapper.toFileBrowserContent
import com.cbgm.sparrow.feature.media.data.mapper.toFileBrowserDirectory
import com.cbgm.sparrow.feature.media.data.mapper.toFileBrowserEntry
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class FileBrowserRepositoryImpl(
    private val dataSource: FileBrowserDataSource
) : FileBrowserRepository {
    override fun hasFileAccess(): Boolean = dataSource.hasFileAccess()

    override suspend fun setRootDirectory(reference: String): Result<Unit> =
        safeSuspendCall { dataSource.setRootDirectory(reference) }

    override suspend fun getRootDirectory(): Result<FileBrowserDirectory> =
        safeSuspendCall { dataSource.getRootDirectory().toFileBrowserDirectory() }

    override suspend fun listDirectory(reference: String): Result<List<FileBrowserEntry>> =
        safeSuspendCall {
            dataSource.listDirectory(reference).map { entry -> entry.toFileBrowserEntry() }
        }

    override suspend fun readFile(reference: String, maxByteSize: Long): Result<FileBrowserContent> =
        safeSuspendCall { dataSource.readFile(reference, maxByteSize).toFileBrowserContent() }
}
