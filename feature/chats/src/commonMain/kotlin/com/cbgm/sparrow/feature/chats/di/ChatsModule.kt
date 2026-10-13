package com.cbgm.sparrow.feature.chats.di

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.feature.chats.data.datasource.MessageHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.repository.DirectConversationReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationHistoryDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupAvatarReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupConversationReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupDescriptionReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupPinReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.repository.GroupVerificationReadRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.overview.datasource.ConversationOverviewDataSource
import com.cbgm.sparrow.feature.chats.data.overview.repository.ConversationOverviewRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.repository.MessageHistoryRepositoryImpl
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.FindMessageHistoryCursorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.LoadOlderMessagesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.contact.EncodeContactForSharingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveConversationQueueAvailabilityUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupExpenseCreationContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.LoadGroupPinnedAttachmentUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupDetailsContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupExpenseSummaryUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupVerificationContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.sparrow.feature.chats.presentation.ContactsFlowViewModel
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationHistoryController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationVoiceController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.DirectIdentityController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.SharedContactController
import com.cbgm.sparrow.feature.chats.presentation.create.CreateGroupViewModel
import com.cbgm.sparrow.feature.chats.presentation.details.GroupVerificationViewModel
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationActionsController
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.forwarding.ForwardingSelectionViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.CreateGroupExpenseViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationActionsController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.GroupPinController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupPollController
import com.cbgm.sparrow.feature.chats.presentation.overview.OverviewViewModel
import com.cbgm.sparrow.feature.chats.presentation.verification.GroupMemberQrVerificationViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.protocol.avatar.GroupAvatarProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {
        registerDirectData()
        registerGroupData()
        registerRepositories()
        registerUseCases()
        registerViewModels()
    }

private fun org.koin.core.module.Module.registerDirectData() {
    singleOf(::DirectConversationDataSource)
}

private fun org.koin.core.module.Module.registerGroupData() {
    singleOf(::GroupConversationHistoryDataSource)
    singleOf(::GroupAvatarDataSource)
    singleOf(::GroupDescriptionDataSource)
    singleOf(::GroupPinDataSource)
}

private fun org.koin.core.module.Module.registerRepositories() {
    singleOf(::DirectConversationReadRepositoryImpl) {
        bind<DirectConversationReadRepository>()
    }
    singleOf(::GroupConversationReadRepositoryImpl) {
        bind<GroupConversationReadRepository>()
    }
    singleOf(::GroupAvatarReadRepositoryImpl) {
        bind<GroupAvatarReadRepository>()
        bind<GroupAvatarProvider>()
    }
    singleOf(::GroupDescriptionReadRepositoryImpl) {
        bind<GroupDescriptionReadRepository>()
    }
    singleOf(::GroupPinReadRepositoryImpl) {
        bind<GroupPinReadRepository>()
        bind<GroupPinnedAttachmentProvider>()
    }
    singleOf(::GroupVerificationReadRepositoryImpl) {
        bind<GroupVerificationReadRepository>()
    }
    singleOf(::ConversationOverviewDataSource)
    singleOf(::ConversationOverviewRepositoryImpl) {
        bind<ConversationOverviewRepository>()
    }
    singleOf(::MessageHistoryDataSource)
    singleOf(::MessageHistoryRepositoryImpl) {
        bind<MessageHistoryRepository>()
    }
}

private fun org.koin.core.module.Module.registerUseCases() {
    singleOf(::LoadOlderMessagesUseCase)
    singleOf(::FindMessageHistoryCursorUseCase)
    singleOf(::EncodeContactForSharingUseCase)
    singleOf(::ObserveDirectConversationUseCase)
    singleOf(::ObserveDirectChatContextUseCase)
    singleOf(::ObserveConversationQueueAvailabilityUseCase)

    singleOf(::ObserveGroupConversationUseCase)
    singleOf(::ObserveGroupChatContextUseCase)
    singleOf(::ObserveGroupDetailsContextUseCase)
    singleOf(::GetGroupExpenseCreationContextUseCase)
    singleOf(::ObserveGroupExpenseSummaryUseCase)
    singleOf(::LoadGroupPinnedAttachmentUseCase)
    singleOf(::ObserveGroupVerificationContextUseCase)
    singleOf(::ObserveGroupVerificationUseCase)

    singleOf(::ObserveConversationOverviewsUseCase)
    singleOf(::ObserveConversationOverviewContextUseCase)
}

private fun org.koin.core.module.Module.registerViewModels() {
    viewModel { CreateGroupExpenseViewModel(savedStateHandle = get(), getContext = get(), orchestrator = get()) }
    factory { ConversationHistoryController(get(), get()) }
    factory { ConversationMediaController(get()) }
    factory { SharedContactController(get()) }
    factory { DirectIdentityController(get(), get()) }

    viewModel {
        ContactsFlowViewModel(orchestrator = get())
    }

    viewModel {
        OverviewViewModel(
            observeConversationContext = get(),
            observeActiveAutoReply = get(),
            orchestrator = get(),
            getGroupLeaveRequirement = get()
        )
    }

    viewModel {
        CreateGroupViewModel(
            savedStateHandle = get(),
            observeContacts = get(),
            orchestrator = get()
        )
    }

    viewModel {
        ForwardingSelectionViewModel(
            observeConversationContext = get(),
            observeContacts = get()
        )
    }

    viewModel {
        val savedStateHandle = get<SavedStateHandle>()
        val media = get<ConversationMediaController>()
        val composer = ConversationComposerController(savedStateHandle, media)
        val actions = GroupConversationActionsController(
            orchestrator = get(),
            composerController = composer,
            mediaController = media,
            voiceController = ConversationVoiceController(get(), get(), media),
            sharedContactController = get()
        )
        val pollController = GroupPollController(get(), get(), get(), media)
        val pinController = GroupPinController(get())
        GroupConversationViewModel(
            savedStateHandle = savedStateHandle,
            observeChatContext = get(),
            observeExpenseSummary = get(),
            indicatorObserver = get(),
            orchestrator = get(),
            observeMessageSafetyAssessments = get(),
            observeVoiceRecordingActive = get(),
            getLocalIdentityName = get(),
            historyController = get(),
            mediaController = media,
            composerController = composer,
            actionsController = actions,
            pollController = pollController,
            pinController = pinController
        )
    }

    viewModel {
        GroupVerificationViewModel(
            savedStateHandle = get(),
            observeGroupDetailsContext = get(),
            orchestrator = get(),
            getContactSafetyNumber = get<GetContactSafetyNumberUseCase>(),
            observeContacts = get(),
            consumeAvatarEditResult = get(),
            getGroupLeaveRequirement = get()
        )
    }

    viewModel {
        GroupMemberQrVerificationViewModel(
            savedStateHandle = get(),
            decodeSharedIdentity = get(),
            getRemoteIdentity = get(),
            orchestrator = get()
        )
    }

    viewModel { (savedStateHandle: SavedStateHandle) ->
        val media = get<ConversationMediaController>()
        val composer = ConversationComposerController(savedStateHandle, media)
        val actions = DirectConversationActionsController(
            orchestrator = get(),
            composerController = composer,
            mediaController = media,
            voiceController = ConversationVoiceController(get(), get(), media),
            sharedContactController = get()
        )
        DirectConversationViewModel(
            savedStateHandle = savedStateHandle,
            observeChatContext = get(),
            orchestrator = get(),
            indicatorObserver = get(),
            observeMessageSafetyAssessments = get(),
            observeVoiceRecordingActive = get(),
            historyController = get(),
            mediaController = media,
            composerController = composer,
            identityController = get(),
            actionsController = actions
        )
    }
}
