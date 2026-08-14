package com.cbgm.sparrow.feature.contacts.data.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.device.DevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.device.DevicePhoneNumberType

internal fun DevicePhoneNumber.toEntity(
    contactId: String,
    updatedAt: Long,
    phoneNumberNormalizer: PhoneNumberNormalizer
): ContactPhoneNumberEntity =
    ContactPhoneNumberEntity(
        id = IdGenerator.generate(),
        contactId = contactId,
        value = value,
        normalizedValue = phoneNumberNormalizer.normalize(value).getOrThrow(),
        type = type.toDomain().name,
        label = label,
        updatedAtEpochMilliseconds = updatedAt
    )

private fun DevicePhoneNumberType.toDomain(): ContactPhoneNumberType =
    when (this) {
        DevicePhoneNumberType.MOBILE -> ContactPhoneNumberType.MOBILE
        DevicePhoneNumberType.WORK_MOBILE -> ContactPhoneNumberType.WORK_MOBILE
        DevicePhoneNumberType.HOME -> ContactPhoneNumberType.HOME
        DevicePhoneNumberType.WORK -> ContactPhoneNumberType.WORK
        DevicePhoneNumberType.MAIN -> ContactPhoneNumberType.MAIN
        DevicePhoneNumberType.CUSTOM -> ContactPhoneNumberType.CUSTOM
        DevicePhoneNumberType.OTHER -> ContactPhoneNumberType.OTHER
    }
