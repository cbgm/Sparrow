package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.base.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact

fun SharedContact.toMessagePart(): Contact =
    Contact(
        id = IdGenerator.generate(prefix = "contact"),
        displayName = displayName,
        phoneNumber = phoneNumber
    )
