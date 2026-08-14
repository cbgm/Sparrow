package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.feature.transport.di.GATEWAY_JSON_QUALIFIER
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named

internal actual fun Module.registerPlatformNodeDirectoryCache() {
    single<NodeDirectoryCache> {
        IosNodeDirectoryCache(
            json = get<Json>(qualifier = named(GATEWAY_JSON_QUALIFIER))
        )
    }
}
