package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

interface GroupMembershipBroadcastDataSource {
    suspend fun enqueueAll(packetsByContactId: Map<String, SparrowPacket>): Result<Unit>
}
