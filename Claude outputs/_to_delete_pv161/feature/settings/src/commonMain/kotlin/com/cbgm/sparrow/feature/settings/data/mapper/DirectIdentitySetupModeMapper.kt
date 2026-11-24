package com.cbgm.sparrow.feature.settings.data.mapper

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode

/** The stored mode; nothing stored or an unknown value means the default, automatic invitation. */
internal fun String?.toDirectIdentitySetupMode(): DirectIdentitySetupMode =
    DirectIdentitySetupMode.entries.firstOrNull { candidate -> candidate.name == this } ?: DirectIdentitySetupMode.AUTOMATIC_INVITATION
