package com.cbgm.sparrow.feature.transport.di

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.controlplane.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneEndpointStore
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.data.controlplane.HttpControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.data.controlplane.HttpControlPlaneHealthMonitor
import com.cbgm.sparrow.feature.transport.data.controlplane.HttpNodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.data.controlplane.InvitationRouteRemoteSource
import com.cbgm.sparrow.feature.transport.data.controlplane.KEY_TRUSTED_DIRECTORY_PUBLIC_KEY
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDirectorySource
import com.cbgm.sparrow.feature.transport.data.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.data.controlplane.PhoneAttestationRemoteSource
import com.cbgm.sparrow.feature.transport.data.controlplane.SignedControlPlaneDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.controlplane.SignedDirectoryControlPlaneCandidateVerifier
import com.cbgm.sparrow.feature.transport.data.controlplane.StoredControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectorySource
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.route.relay.ControlPlaneRelayPolicy
import com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayClient
import com.cbgm.sparrow.feature.transport.data.route.relay.OnionRelayEngine
import com.cbgm.sparrow.feature.transport.data.route.relay.createControlPlaneHttpClient
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneHealthMonitor
import com.cbgm.sparrow.feature.transport.domain.gateway.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.domain.gateway.InvitationMailbox
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalBootstrapRoutingIdSource
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The Control Planes: their addresses, health, directory and the requests that go to them through relays. */
internal val controlPlaneModule =
    module {
        single<StoredControlPlaneConfiguration> {
            StoredControlPlaneConfiguration(dataStore = get(), nodeDirectoryCache = get())
        }

        single<ControlPlaneEndpointStore> {
            get<StoredControlPlaneConfiguration>()
        }

        single<ControlPlaneConfiguration> {
            get<StoredControlPlaneConfiguration>()
        }

        single<ControlPlaneStatusStore> {
            get<StoredControlPlaneConfiguration>()
        }

        single {
            ControlPlaneRelayPolicy(
                transportConfig = get<TransportConfig>(),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                cache = get<NodeDirectoryCache>()
            )
        }

        // Requests to Control Planes go through two relays, so no Control Plane sees the app's address.
        single<HttpClient>(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)) {
            val policy = get<ControlPlaneRelayPolicy>()
            createControlPlaneHttpClient(
                engine =
                    OnionRelayEngine(
                        onion = get<OnionRelayClient>(),
                        directClient = get<HttpClient>().config { expectSuccess = false },
                        requirement = policy::requirement
                    ),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            AliasClaimRegistrar(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                relayPolicy = get<ControlPlaneRelayPolicy>(),
                directoryVerifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>(),
                signingKeyPairSource = get<LocalSigningKeyPairSource>(),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                localBootstrapRoutingIdSource = get<LocalBootstrapRoutingIdSource>(),
                localPhoneNumberSource = get<LocalPhoneNumberSource>(),
                phoneAttestationRemoteSource = get<PhoneAttestationRemoteSource>(),
                invitationMailbox = get<InvitationMailbox>(),
                encryptionKeyPairSource = get<LocalEncryptionKeyPairSource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            InvitationRouteRemoteSource(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                work = MailboxCreationWorkSolver(get<CryptoHash>()),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            PhoneAttestationRemoteSource(
                controlPlaneHttpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                controlPlaneConfiguration = get<ControlPlaneEndpointStore>(),
                pinnedDirectoryKey = { get<SparrowDataStore>().getString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY) },
                signatureCrypto = get<DetachedSignatureCrypto>(),
                cipher = get<SealedMessageCipher>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single {
            ControlPlaneRequestRouter(
                configuration = get<ControlPlaneEndpointStore>(),
                statusStore = get<ControlPlaneStatusStore>()
            )
        }

        single<ControlPlaneHealthMonitor> {
            HttpControlPlaneHealthMonitor(
                httpClient = get(qualifier = named(CONTROL_PLANE_HTTP_CLIENT_QUALIFIER)),
                configuration = get<ControlPlaneEndpointStore>(),
                statusStore = get<ControlPlaneStatusStore>()
            )
        }

        single {
            SignedControlPlaneDirectoryVerifier(
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                signatureCrypto = get<DetachedSignatureCrypto>(),
                hash = get<CryptoHash>()
            )
        }

        single<ControlPlaneDirectorySynchronizer> {
            HttpControlPlaneDirectorySynchronizer(
                httpClient = get<HttpClient>(),
                configuration = get<ControlPlaneEndpointStore>(),
                verifier = get<SignedControlPlaneDirectoryVerifier>(),
                dataStore = get(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER))
            )
        }

        single<NodeControlPlaneDirectorySource> {
            HttpNodeControlPlaneDirectorySource(
                httpClient = get<HttpClient>()
            )
        }

        single<ControlPlaneCandidateVerifier> {
            SignedDirectoryControlPlaneCandidateVerifier(
                nodeDirectorySource = get<NodeDirectorySource>(),
                json = get(qualifier = named(GATEWAY_JSON_QUALIFIER)),
                verifier = get<NodeDirectoryVerifier>(),
                transportConfig = get<TransportConfig>()
            )
        }

        single {
            NodeControlPlaneDiscoverySynchronizer(
                source = get<NodeControlPlaneDirectorySource>(),
                candidateVerifier = get<ControlPlaneCandidateVerifier>(),
                configuration = get<ControlPlaneEndpointStore>()
            )
        }
    }
