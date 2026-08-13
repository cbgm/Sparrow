package com.cbgm.securechat.feature.contacts.domain.model.device

/**
 * One phone number belonging to a device contact.
 *
 * [label] is mainly useful for custom values such as:
 *
 * - "Private"
 * - "Office Berlin"
 * - "Emergency"
 */
data class DevicePhoneNumber(
    val value: String,
    val type: DevicePhoneNumberType,
    val label: String?
)
