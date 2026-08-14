package com.cbgm.securechat.feature.contacts.domain.repository

fun interface DeviceContactsPermissionRepository {
    fun canReadContacts(): Boolean
}
