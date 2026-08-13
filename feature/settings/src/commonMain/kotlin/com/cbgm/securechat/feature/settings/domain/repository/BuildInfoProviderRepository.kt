package com.cbgm.securechat.feature.settings.domain.repository

import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

interface BuildInfoProviderRepository {
    val build: BuildInfo
}
