package com.cbgm.sparrow.feature.settings.domain.model

data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val gitSha: String?
)
