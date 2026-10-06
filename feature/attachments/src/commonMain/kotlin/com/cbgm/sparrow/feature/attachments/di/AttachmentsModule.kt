package com.cbgm.sparrow.feature.attachments.di

import com.cbgm.sparrow.feature.attachments.data.datasource.AttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.BlobTransferDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.repository.MessageAttachmentOperationsRepositoryImpl
import com.cbgm.sparrow.feature.attachments.data.repository.MessageAttachmentRepositoryImpl
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteConversationLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.DeleteLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentContentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveAttachmentStorageSummariesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveLocalAttachmentsUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.SaveMessageAttachmentTranscriptUseCase
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
        singleOf(::LocalAttachmentDataSource)
        singleOf(::MessageAttachmentDataSource)
        singleOf(::AttachmentContentDataSource)
        singleOf(::MessageAttachmentCacheCoordinator)
        singleOf(::MessageAttachmentOperationsRepositoryImpl) {
            bind<MessageAttachmentOperationsRepository>()
        }
        singleOf(::MessageAttachmentRepositoryImpl) {
            bind<MessageAttachmentRepository>()
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
                part = parameters.get(),
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
