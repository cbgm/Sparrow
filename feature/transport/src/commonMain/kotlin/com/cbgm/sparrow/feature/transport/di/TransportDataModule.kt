package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.connection.CountingConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.data.http.createPlatformHttpClient
import com.cbgm.sparrow.feature.transport.data.mailbox.createGatewayJson
import com.cbgm.sparrow.feature.transport.data.preferences.DataStoreTransportPreferences
import com.cbgm.sparrow.feature.transport.data.routing.PhoneNumberRoutingIdSource
import com.cbgm.sparrow.feature.transport.data.routing.Sha256RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.data.routing.SigningKeyRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.ConnectionKeepAlive
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalBootstrapRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.RoutingIdGenerator
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
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

        single<LocalRoutingIdSource> {
            SigningKeyRoutingIdSource(
                localSigningPublicKeySource = get<LocalSigningPublicKeySource>(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<LocalBootstrapRoutingIdSource> {
            PhoneNumberRoutingIdSource(
                localPhoneNumberSource = get(),
                routingIdGenerator = get<RoutingIdGenerator>()
            )
        }

        single<TransportPreferences> { DataStoreTransportPreferences(dataStore = get()) }

        single<ConnectionKeepAlive> { CountingConnectionKeepAlive() }
    }
