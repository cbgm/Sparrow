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
import com.cbgm.sparrow.feature.chats.domain.usecase.group.LoadGroupPinnedAttachmentUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupDetailsContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupExpensesUseCase
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
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationStateController
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.direct.DirectIndicatorController
import com.cbgm.sparrow.feature.chats.presentation.forwarding.ForwardingSelectionViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationActionsController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationStateController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupConversationViewModel
import com.cbgm.sparrow.feature.chats.presentation.group.GroupExpenseController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupIndicatorController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupPinController
import com.cbgm.sparrow.feature.chats.presentation.group.GroupPollController
import com.cbgm.sparrow.feature.chats.presentation.overview.OverviewViewModel
import com.cbgm.sparrow.feature.chats.presentation.verification.GroupMemberQrVerificationViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.protocol.avatar.GroupAvatarProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinViewModelScopeApi
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.viewmodel.scope.viewModelScope

val chatsModule =
    module {
        registerDirectData()
        registerGroupData()
        registerRepositories()
        registerUseCases()
        registerViewModelControllers()
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
    singleOf(::ObserveGroupExpensesUseCase)
    singleOf(::ObserveGroupChatContextUseCase)
    singleOf(::ObserveGroupDetailsContextUseCase)
    singleOf(::LoadGroupPinnedAttachmentUseCase)
    singleOf(::ObserveGroupVerificationContextUseCase)
    singleOf(::ObserveGroupVerificationUseCase)

    singleOf(::ObserveConversationOverviewsUseCase)
    singleOf(::ObserveConversationOverviewContextUseCase)
}

@OptIn(KoinExperimentalAPI::class, KoinViewModelScopeApi::class)
private fun org.koin.core.module.Module.registerViewModelControllers() {
    viewModelScope {
        scopedOf(::ConversationMediaController)
        scopedOf(::ConversationComposerController)
        scopedOf(::ConversationHistoryController)
        scopedOf(::ConversationVoiceController)
        scopedOf(::SharedContactController)
        scopedOf(::DirectIdentityController)
        scopedOf(::DirectIndicatorController)
        scopedOf(::DirectConversationStateController)
        scopedOf(::DirectConversationActionsController)
        scopedOf(::GroupIndicatorController)
        scopedOf(::GroupConversationStateController)
        scopedOf(::GroupConversationActionsController)
        scopedOf(::GroupPollController)
        scopedOf(::GroupPinController)
        scopedOf(::GroupExpenseController)
    }

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
        GroupConversationViewModel(
            savedStateHandle = get(),
            observeChatContext = get(),
            observeExpenseSummary = get(),
            observeVoiceRecordingActive = get(),
            getLocalIdentityName = get(),
            historyController = get(),
            mediaController = get(),
            composerController = get(),
            indicatorController = get(),
            stateController = get(),
            actionsController = get(),
            pollController = get(),
            pinController = get(),
            expenseController = get()
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
        DirectConversationViewModel(
            savedStateHandle = savedStateHandle,
            observeChatContext = get(),
            orchestrator = get(),
            observeVoiceRecordingActive = get(),
            historyController = get(),
            mediaController = get(),
            composerController = get(),
            identityController = get(),
            indicatorController = get(),
            stateController = get(),
            actionsController = get()
        )
    }
}
