package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartAttachmentConversationNameObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "attachment conversation-name observer"

    override suspend fun runAfterIdentityReady() {
        initialization.attachmentConversationNameObserver.run()
    }
}
