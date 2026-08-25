package com.cbgm.sparrow.feature.attachments.di

import com.cbgm.sparrow.feature.attachments.data.datasource.BlobTransferDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.repository.BlobTransferRepositoryImpl
import com.cbgm.sparrow.feature.attachments.data.repository.MessageAttachmentRepositoryImpl
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DownloadBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.UploadBlobUseCase
import com.cbgm.sparrow.feature.attachments.runtime.MessageAttachmentCacheCoordinator
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val attachmentsModule =
    module {
        singleOf(::BlobTransferDataSource)
        singleOf(::BlobTransferRepositoryImpl) {
            bind<BlobTransferRepository>()
        }
        singleOf(::MessageAttachmentDataSource)
        singleOf(::MessageAttachmentCacheCoordinator)
        singleOf(::MessageAttachmentRepositoryImpl) {
            bind<MessageAttachmentRepository>()
        }

        factory {
            UploadBlobUseCase(repository = get<BlobTransferRepository>())
        }
        factory {
            DownloadBlobUseCase(repository = get<BlobTransferRepository>())
        }
        factory {
            DeleteBlobUseCase(repository = get<BlobTransferRepository>())
        }
        factory {
            LoadMessageAttachmentUseCase(repository = get<MessageAttachmentRepository>())
        }
    }
