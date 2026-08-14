package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.feature.transport.di.GATEWAY_JSON_QUALIFIER
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named

internal actual fun Module.registerPlatformNodeDirectoryCache() {
    single<NodeDirectoryCache> {
        AndroidNodeDirectoryCache(
            context = androidContext(),
            json = get<Json>(qualifier = named(GATEWAY_JSON_QUALIFIER))
        )
    }
}
