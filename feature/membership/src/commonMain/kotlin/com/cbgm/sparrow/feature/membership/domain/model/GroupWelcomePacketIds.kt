package com.cbgm.sparrow.feature.membership.domain.model

/** Membership mints welcome packet IDs as "group-welcome-<groupId>-<sourceId>-<epoch>". */
object GroupWelcomePacketIds {
    fun of(groupId: String, sourceId: String, epoch: Int): String = "group-welcome-$groupId-$sourceId-$epoch"

    /** The membership source ID inside [welcomePacketId], or null when it is no welcome of [groupId] at [epoch]. */
    fun sourceIdOf(welcomePacketId: String, groupId: String, epoch: Int): String? {
        val prefix = "group-welcome-$groupId-"
        val suffix = "-$epoch"
        return welcomePacketId
            .takeIf { it.startsWith(prefix) && it.endsWith(suffix) }
            ?.removePrefix(prefix)
            ?.removeSuffix(suffix)
    }
}
