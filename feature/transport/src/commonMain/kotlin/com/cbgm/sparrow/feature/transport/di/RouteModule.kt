package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.route.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.data.route.RelaySelector
import com.cbgm.sparrow.feature.transport.data.route.RouteProtectionRepositoryImpl
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayClient
import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort
import com.cbgm.sparrow.feature.transport.domain.port.DirectSendingPort
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.repository.RouteProtectionRepository
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/** The onion routes: relay hops, circuits and single relayed requests. */
internal val routeModule =
    module {
        // One selector for the HTTP relay and the circuits, so a relay that failed for one is left out by both.
        single {
            RelaySelector(
                cache = get<NodeDirectoryCache>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                preferences = get(),
                // Read when a route is built: the connection manager itself uses the relayed client.
                connectedNodeId = { get<TransportConnectionPort>().connectedNodeId }
            )
        }

        single {
            CircuitRunner(
                httpClient = get<HttpClient>(),
                cipher = get<CircuitCipher>(),
                selector = get<RelaySelector>(),
                preferences = get()
            )
        } bind CircuitPort::class

        single<DirectSendingPort> { DirectSendingPolicy(circuits = get(), preferences = get()) }

        single {
            OnionRelayClient(
                selector = get<RelaySelector>(),
                cipher = get<SealedMessageCipher>(),
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<RouteProtectionRepository> { RouteProtectionRepositoryImpl(circuits = get(), preferences = get()) }
    }
