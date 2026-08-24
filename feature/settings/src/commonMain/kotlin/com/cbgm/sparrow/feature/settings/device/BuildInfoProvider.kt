package com.cbgm.sparrow.feature.settings.device

import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo

interface BuildInfoProvider {
    val build: BuildInfo
}
