package com.cbgm.sparrow.feature.attachments.di

import com.cbgm.sparrow.feature.attachments.data.datasource.AttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.BlobTransferDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.repository.BlobTransferRepositoryImpl
import com.cbgm.sparrow.feature.attachments.data.repository.MessageAttachmentOperationsRepositoryImpl
import com.cbgm.sparrow.feature.attachments.data.repository.MessageAttachmentRepositoryImpl
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteConversationLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DownloadBlobUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentContentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveAttachmentStorageSummariesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.SaveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.UploadBlobUseCase
import com.cbgm.sparrow.feature.attachments.presentation.AttachmentViewModel
import com.cbgm.sparrow.feature.attachments.presentation.management.AttachmentManagementViewModel
import com.cbgm.sparrow.feature.attachments.presentation.storage.AttachmentStorageViewModel
import com.cbgm.sparrow.feature.attachments.runtime.MessageAttachmentCacheCoordinator
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val attachmentsModule =
    module {
        singleOf(::BlobTransferDataSource)
        singleOf(::BlobTransferRepositoryImpl) {
            bind<BlobTransferRepository>()
        }
        singleOf(::LocalAttachmentDataSource)
        singleOf(::MessageAttachmentDataSource)
        singleOf(::AttachmentContentDataSource)
        singleOf(::LocalAttachmentContentDataSource)
        singleOf(::MessageAttachmentCacheCoordinator)
        singleOf(::MessageAttachmentOperationsRepositoryImpl) {
            bind<MessageAttachmentOperationsRepository>()
        }
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
        factory {
            LoadAttachmentBytesUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            LoadAttachmentContentUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            SaveMessageAttachmentTranscriptUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            ObserveMessageAttachmentTranscriptUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            ObserveLocalAttachmentsUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            ObserveAttachmentStorageSummariesUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            DeleteLocalAttachmentsUseCase(repository = get<MessageAttachmentRepository>())
        }
        factory {
            DeleteConversationLocalAttachmentsUseCase(repository = get<MessageAttachmentRepository>())
        }

        viewModel { parameters ->
            AttachmentViewModel(
                target = parameters.get<AttachmentTarget>(),
                loadAttachmentContent = get()
            )
        }

        viewModel {
            AttachmentStorageViewModel(observeAttachmentStorageSummaries = get())
        }
        viewModel {
            AttachmentManagementViewModel(
                savedStateHandle = get(),
                observeLocalAttachments = get(),
                deleteLocalAttachments = get()
            )
        }
    }
