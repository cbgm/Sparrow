package com.cbgm.sparrow.feature.contacts.data.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber

internal fun ImportDevicePhoneNumber.toEntity(
    contactId: String,
    updatedAtEpochMilliseconds: Long,
    phoneNumberNormalizer: PhoneNumberNormalizer
): ContactPhoneNumberEntity =
    ContactPhoneNumberEntity(
        id = IdGenerator.generate(),
        contactId = contactId,
        value = value,
        normalizedValue = phoneNumberNormalizer.normalize(value).getOrThrow(),
        type = type.name,
        label = label,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
