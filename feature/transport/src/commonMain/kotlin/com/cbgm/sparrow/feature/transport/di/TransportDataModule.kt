package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.CountingConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.data.http.createPlatformHttpClient
import com.cbgm.sparrow.feature.transport.data.mailbox.createGatewayJson
import com.cbgm.sparrow.feature.transport.data.preferences.DataStoreTransportPreferencesLocalDataSource
import com.cbgm.sparrow.feature.transport.data.preferences.TransportPreferencesLocalDataSource
import com.cbgm.sparrow.feature.transport.data.preferences.TransportPreferencesRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.routing.KeyRoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.routing.Sha256RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.domain.port.ConnectionKeepAlivePort
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeySource
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Configuration, HTTP, preferences and routing IDs the other parts share. */
internal val transportDataModule =
    module {
        single {
            TransportConfig()
        }

        single<HttpClient> {
            createPlatformHttpClient(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<Json>(qualifier = named(GATEWAY_JSON_QUALIFIER)) {
            createGatewayJson()
        }

        single<RoutingIdGenerator> {
            Sha256RoutingIdGenerator(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single<RoutingIdLocalDataSource> {
            KeyRoutingIdLocalDataSource(
                localSigningPublicKeySource = get<LocalSigningPublicKeySource>(),
                localPhoneNumberSource = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<RoutingIdRepository> { RoutingIdRepositoryImpl(localDataSource = get<RoutingIdLocalDataSource>()) }

        single<TransportPreferencesLocalDataSource> { DataStoreTransportPreferencesLocalDataSource(dataStore = get()) }

        single<TransportPreferencesRepository> {
            TransportPreferencesRepositoryImpl(localDataSource = get<TransportPreferencesLocalDataSource>())
        }

        single<ConnectionKeepAlivePort> { CountingConnectionKeepAlive() }
    }
