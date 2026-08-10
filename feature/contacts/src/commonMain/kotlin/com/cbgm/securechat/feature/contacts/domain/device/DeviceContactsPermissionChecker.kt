package com.cbgm.securechat.feature.contacts.domain.device

fun interface DeviceContactsPermissionChecker {
    fun canReadContacts(): Boolean
}
