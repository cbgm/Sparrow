package com.cbgm.sparrow.feature.contacts.data.mapper

import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.model.ContactWithPhoneNumbersDto
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus

fun ContactWithPhoneNumbersDto.toContact(): Contact =
    Contact(
        id = contact.id,
        displayName = contact.displayName,
        phoneNumbers =
            phoneNumbers.map { phoneNumber ->
                phoneNumber.toContactPhoneNumber()
            },
        preferredPhoneNumberId = contact.preferredPhoneNumberId,
        deviceContactId =
            contact.deviceContactId,
        deviceContactLinkStatus = contact.deviceContactLinkStatus.toDeviceContactLinkStatus(),
        sparrowIdentity = null,
        createdAtEpochMilliseconds = contact.createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = contact.updatedAtEpochMilliseconds
    )

private fun ContactPhoneNumberEntity.toContactPhoneNumber(): ContactPhoneNumber =
    ContactPhoneNumber(
        id = id,
        value = value,
        type = type.toContactPhoneNumberType(),
        label = label
    )

private fun String.toContactPhoneNumberType(): ContactPhoneNumberType =
    when (this) {
        ContactPhoneNumberType.MOBILE.name ->
            ContactPhoneNumberType.MOBILE

        ContactPhoneNumberType.WORK_MOBILE.name ->
            ContactPhoneNumberType.WORK_MOBILE

        ContactPhoneNumberType.HOME.name ->
            ContactPhoneNumberType.HOME

        ContactPhoneNumberType.WORK.name ->
            ContactPhoneNumberType.WORK

        ContactPhoneNumberType.MAIN.name ->
            ContactPhoneNumberType.MAIN

        ContactPhoneNumberType.CUSTOM.name ->
            ContactPhoneNumberType.CUSTOM

        ContactPhoneNumberType.OTHER.name ->
            ContactPhoneNumberType.OTHER

        else ->
            error("Unknown contact phone-number type: $this")
    }

private fun String.toDeviceContactLinkStatus(): DeviceContactLinkStatus =
    when (this) {
        DeviceContactLinkStatus.NOT_LINKED.name ->
            DeviceContactLinkStatus.NOT_LINKED

        DeviceContactLinkStatus.LINKED.name ->
            DeviceContactLinkStatus.LINKED

        DeviceContactLinkStatus.MISSING.name ->
            DeviceContactLinkStatus.MISSING

        else ->
            error("Unknown device-contact link status: $this")
    }
