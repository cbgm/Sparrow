package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveConversationOverviewContextUseCaseTest {
    @Test
    fun exposesConversationOverviewWithoutLoadingAvatars() =
        runTest {
            val conversations =
                listOf(
                    directConversation(),
                    groupConversation()
                )
            val useCase =
                ObserveConversationOverviewContextUseCase(
                    conversationRepository = FakeConversationOverviewRepository(flowOf(conversations))
                )

            val context = useCase().first()

            assertEquals(conversations, context.conversations)
        }

    private fun directConversation(): ConversationOverview =
        ConversationOverview(
            id = "conversation-1",
            contactId = "contact-1",
            displayName = "Contact",
            lastMessageText = "message",
            lastMessageTimestamp = 1L,
            updatedAtEpochMilliseconds = 1L,
            unreadCount = 0,
            participantCount = 2,
            type = ConversationOverviewType.DIRECT
        )

    private fun groupConversation(): ConversationOverview =
        ConversationOverview(
            id = "group-1",
            contactId = "",
            displayName = "Group",
            lastMessageText = "message",
            lastMessageTimestamp = 1L,
            updatedAtEpochMilliseconds = 1L,
            unreadCount = 0,
            participantCount = 3,
            type = ConversationOverviewType.GROUP
        )

    private class FakeConversationOverviewRepository(
        private val conversations: Flow<List<ConversationOverview>>
    ) : ConversationOverviewRepository {
        override fun observeAll(): Flow<List<ConversationOverview>> = conversations

        override suspend fun incrementUnseenLocalMessageCount(conversationId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun clearUnseenLocalMessageCount(conversationId: String): Result<Unit> =
            Result.success(Unit)
    }
}
