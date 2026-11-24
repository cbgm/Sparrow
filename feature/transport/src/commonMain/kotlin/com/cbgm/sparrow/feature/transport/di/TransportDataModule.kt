package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.CountingConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.data.http.createPlatformHttpClient
import com.cbgm.sparrow.feature.transport.data.mailbox.createGatewayJson
import com.cbgm.sparrow.feature.transport.data.preferences.TransportPreferencesRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.routing.LocalBootstrapRoutingIdRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.routing.LocalRoutingIdRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.routing.Sha256RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.domain.port.ConnectionKeepAlivePort
import com.cbgm.sparrow.feature.transport.domain.repository.LocalBootstrapRoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.repository.LocalRoutingIdRepository
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

        single<LocalRoutingIdRepository> {
            LocalRoutingIdRepositoryImpl(
                localSigningPublicKeySource = get<LocalSigningPublicKeySource>(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<LocalBootstrapRoutingIdRepository> {
            LocalBootstrapRoutingIdRepositoryImpl(
                localPhoneNumberSource = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<TransportPreferencesRepository> { TransportPreferencesRepositoryImpl(dataStore = get()) }

        single<ConnectionKeepAlivePort> { CountingConnectionKeepAlive() }
    }
