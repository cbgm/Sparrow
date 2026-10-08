package com.cbgm.sparrow.runtime.startup

import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.task.InitializeAppLanguageStartupTask
import com.cbgm.sparrow.runtime.startup.task.InitializeCryptoRuntimeStartupTask
import com.cbgm.sparrow.runtime.startup.task.InitializeLocalIntelligenceStartupTask
import com.cbgm.sparrow.runtime.startup.task.InitializeNotificationRuntimeStartupTask
import com.cbgm.sparrow.runtime.startup.task.LoadControlPlaneConfigurationStartupTask
import com.cbgm.sparrow.runtime.startup.task.MaintainControlPlaneDirectoryStartupTask
import com.cbgm.sparrow.runtime.startup.task.MaintainControlPlaneHealthStartupTask
import com.cbgm.sparrow.runtime.startup.task.ObserveControlPlaneRegistrationTargetsStartupTask
import com.cbgm.sparrow.runtime.startup.task.ResolveIdentityStatusStartupTask
import com.cbgm.sparrow.runtime.startup.task.RestoreControlPlaneDirectoryStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartApprovedIdentityReconnectionObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartAttachmentConversationNameObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartContactBlockObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartConversationNotificationCoordinatorStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartDirectIdentityResultObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartInvitationResultObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartMembershipResultObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.StartMessagingTransportResultObserverStartupTask
import com.cbgm.sparrow.runtime.startup.task.SynchronizeDeviceContactsStartupTask

fun createApplicationStartupTasks(
    initAppLanguageUseCase: InitAppLanguageUseCase,
    initialization: AppInitializationDependencies
): List<StartupTask> =
    listOf(
        InitializeAppLanguageStartupTask(initAppLanguageUseCase),
        InitializeCryptoRuntimeStartupTask(initialization),
        LoadControlPlaneConfigurationStartupTask(initialization),
        ResolveIdentityStatusStartupTask(initialization),
        RestoreControlPlaneDirectoryStartupTask(initialization),
        InitializeNotificationRuntimeStartupTask(initialization),
        StartConversationNotificationCoordinatorStartupTask(initialization),
        StartAttachmentConversationNameObserverStartupTask(initialization),
        StartInvitationResultObserverStartupTask(initialization),
        StartMembershipResultObserverStartupTask(initialization),
        StartMessagingTransportResultObserverStartupTask(initialization),
        StartDirectIdentityResultObserverStartupTask(initialization),
        StartContactBlockObserverStartupTask(initialization),
        StartApprovedIdentityReconnectionObserverStartupTask(initialization),
        MaintainControlPlaneDirectoryStartupTask(initialization),
        MaintainControlPlaneHealthStartupTask(initialization),
        ObserveControlPlaneRegistrationTargetsStartupTask(initialization),
        SynchronizeDeviceContactsStartupTask(initialization),
        InitializeLocalIntelligenceStartupTask(initialization)
    )
