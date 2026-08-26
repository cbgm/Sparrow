package com.cbgm.sparrow.feature.media.data.repository

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.mapper.toDomain
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class FileBrowserRepositoryImpl(
    private val dataSource: FileBrowserDataSource
) : FileBrowserRepository {
    override fun hasFileAccess(): Boolean = dataSource.hasFileAccess()

    override suspend fun setRootDirectory(reference: String): Result<Unit> =
        dataSource.setRootDirectory(reference)

    override suspend fun getRootDirectory(): Result<FileBrowserDirectory> =
        dataSource.getRootDirectory().map { directory -> directory.toDomain() }

    override suspend fun listDirectory(reference: String): Result<List<FileBrowserEntry>> =
        dataSource.listDirectory(reference).map { entries -> entries.map { entry -> entry.toDomain() } }

    override suspend fun readFile(reference: String, maxByteSize: Long): Result<FileBrowserContent> =
        dataSource.readFile(reference, maxByteSize).map { content -> content.toDomain() }
}
