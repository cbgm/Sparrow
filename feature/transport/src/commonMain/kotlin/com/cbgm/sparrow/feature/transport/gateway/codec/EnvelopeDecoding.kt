package com.cbgm.sparrow.feature.transport.gateway.codec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Envelopes read one by one. Anyone can address an envelope to a user, so one unreadable entry must neither hide
 * the others nor end a session; its ID, when still readable, is kept so it can be acknowledged and dropped instead
 * of being delivered again on every reconnect until it expires.
 */
internal class DecodedEnvelopes<T>(
    val envelopes: List<T>,
    val unreadableEnvelopeIds: List<String>
)

internal inline fun <reified T> Json.decodeEnvelopes(elements: JsonArray): DecodedEnvelopes<T> {
    val envelopes = mutableListOf<T>()
    val unreadableEnvelopeIds = mutableListOf<String>()
    elements.forEach { element ->
        runCatching { decodeFromJsonElement<T>(element) }
            .onSuccess(envelopes::add)
            .onFailure { element.envelopeIdOrNull()?.let(unreadableEnvelopeIds::add) }
    }
    return DecodedEnvelopes(envelopes, unreadableEnvelopeIds)
}

/** The `envelopeId` of an envelope that could not be read, if it is still a non-blank string. */
internal fun JsonElement.envelopeIdOrNull(): String? =
    ((this as? JsonObject)?.get("envelopeId") as? JsonPrimitive)
        ?.takeIf(JsonPrimitive::isString)
        ?.content
        ?.takeIf(String::isNotBlank)
