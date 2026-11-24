package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneEndpointStore
import com.cbgm.sparrow.feature.transport.data.directory.DataStoreNodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.directory.DirectoryNodeEndpointResolver
import com.cbgm.sparrow.feature.transport.data.directory.HttpNodeDirectorySource
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointSelector
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.domain.gateway.NodeEndpointResolver
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The signed node directory: fetching, verifying, caching and picking gateway nodes. */
internal val directoryModule =
    module {
        single<NodeDirectoryCache> {
            DataStoreNodeDirectoryCache(
                dataStore = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            NodeDirectoryVerifier(
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cryptoHash = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            NodeEndpointSelector(config = get<TransportConfig>())
        }

        single<NodeEndpointResolver> {
            DirectoryNodeEndpointResolver(
                source = HttpNodeDirectorySource(httpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER))),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                cache = get(),
                verifier = get(),
                config = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                controlPlaneStatusStore = get<ControlPlaneStatusStore>(),
                endpointSelector = get<NodeEndpointSelector>(),
                controlPlaneDirectorySynchronizer = get<ControlPlaneDirectorySynchronizer>(),
                preferences = get()
            )
        }

        single<NodeDirectorySource> {
            HttpNodeDirectorySource(httpClient = get<HttpClient>())
        }
    }
