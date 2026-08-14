package com.cbgm.sparrow.feature.contacts.domain.repository

fun interface DeviceContactsPermissionRepository {
    fun canReadContacts(): Boolean
}
