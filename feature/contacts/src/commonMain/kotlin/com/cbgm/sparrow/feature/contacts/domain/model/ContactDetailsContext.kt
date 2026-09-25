package com.cbgm.sparrow.feature.contacts.domain.model

import com.cbgm.sparrow.core.crypto.safety.SafetyNumber

data class ContactDetailsContext(
    val contact: Contact?,
    val safetyNumber: SafetyNumber?
)
