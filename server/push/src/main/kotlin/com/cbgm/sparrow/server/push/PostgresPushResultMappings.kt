package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.TransportEnvelope
import java.sql.ResultSet

internal fun ResultSet.readPushDevices(): List<PushDevice> =
    buildList {
        while (next()) {
            add(
                PushDevice(
                    routingId = getString("routing_id"),
                    token = getString("token"),
                    platform = getString("platform")
                )
            )
        }
    }

internal fun ResultSet.readTransportEnvelopes(): List<TransportEnvelope> =
    buildList {
        while (next()) {
            add(
                TransportEnvelope(
                    version = getInt("version"),
                    envelopeId = getString("envelope_id"),
                    senderId = getString("sender_id"),
                    recipientId = getString("recipient_id"),
                    payload = getString("payload"),
                    createdAtEpochMilliseconds = getLong("created_at_epoch_milliseconds")
                )
            )
        }
    }

internal fun ResultSet.readRecipientIds(): Set<String> =
    buildSet {
        while (next()) {
            add(getString("recipient_id"))
        }
    }
