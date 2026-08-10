package com.cbgm.securechat.feature.settings.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider

class AndroidBuildInfoProvider(
    private val context: Context
) : BuildInfoProvider {
    override val build: BuildInfo
        get() {
            val packageInfo =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            val isDebuggable =
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

            return BuildInfo(
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = packageInfo.longVersionCode.toInt(),
                buildType = if (isDebuggable) DEBUG_BUILD_TYPE else RELEASE_BUILD_TYPE,
                gitSha = ""
            )
        }

    private companion object {
        const val DEBUG_BUILD_TYPE = "debug"
        const val RELEASE_BUILD_TYPE = "release"
    }
}
