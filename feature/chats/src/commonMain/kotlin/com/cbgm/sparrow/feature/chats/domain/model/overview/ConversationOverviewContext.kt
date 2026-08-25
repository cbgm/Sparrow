package com.cbgm.sparrow.feature.chats.domain.model.overview

data class ConversationOverviewContext(
    val conversations: List<ConversationOverview>,
    val profilePictures: Map<String, ByteArray?>,
    val groupAvatars: Map<String, ByteArray?>
)
