package com.cbgm.sparrow.feature.settings.data.storage

import org.koin.core.module.Module

internal actual fun Module.registerPlatformSettingsStorage() {
    single<SettingsStorage> {
        IosSettingsStorage()
    }
}
