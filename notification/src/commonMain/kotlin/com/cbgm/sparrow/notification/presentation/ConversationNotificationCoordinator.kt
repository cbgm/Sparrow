package com.cbgm.sparrow.notification.presentation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.notification.domain.model.ConversationNotificationEvent
import com.cbgm.sparrow.notification.domain.usecase.ObserveConversationNotificationEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConversationNotificationCoordinator(
    private val observeConversationNotificationEvents: ObserveConversationNotificationEventsUseCase,
    private val presenter: ConversationNotificationPresenter
) {
    private val logger = SparrowLog.withTag("ConversationNotificationCoordinator")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var collectionJob: Job? = null

    fun start() {
        if (collectionJob?.isActive == true) {
            return
        }

        collectionJob =
            scope
                .launch {
                    observeConversationNotificationEvents().collect { event ->
                        when (event) {
                            is ConversationNotificationEvent.Show -> {
                                presenter.show(event.notification)
                            }

                            is ConversationNotificationEvent.Cancel -> {
                                presenter.cancel(event.conversationId)
                            }
                        }
                    }
                }.also { job ->
                    job.invokeOnCompletion { error ->
                        if (error != null) {
                            logger.error(error) { "Conversation notification observation stopped" }
                        }
                    }
                }
    }
}
