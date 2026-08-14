package com.cbgm.sparrow.feature.settings.domain.repository

import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo

interface BuildInfoProviderRepository {
    val build: BuildInfo
}
