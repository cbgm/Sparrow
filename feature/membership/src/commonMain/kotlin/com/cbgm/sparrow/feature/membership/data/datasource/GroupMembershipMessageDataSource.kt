package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.entity.MessageEntity

interface GroupMembershipMessageDataSource {
    fun memberAdded(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): MessageEntity

    fun memberRemoved(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): MessageEntity

    fun memberLeft(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): MessageEntity

    fun localMembershipLeft(
        conversationId: String,
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): MessageEntity

    fun localConversationDeletedMarker(
        conversationId: String,
        createdAtEpochMilliseconds: Long
    ): MessageEntity
}
