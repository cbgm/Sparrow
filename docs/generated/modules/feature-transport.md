# `:feature:transport`

Source directory: `feature/transport`

## Direct project dependencies

- `:core`
- `:data:datastore`
- `:core:crypto`
- `:core:protocol`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `TransportConfig` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/config/TransportConfig.kt` |
| `DefaultTransportConnectionManager` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/DefaultTransportConnectionManager.kt` |
| `TransportConnectionManager` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionManager.kt` |
| `TransportConnectionState` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportConnectionState.kt` |
| `TransportDiagnosticsState` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/connection/TransportDiagnosticsState.kt` |
| `ControlPlaneCandidateVerifier` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `ControlPlaneConfigurationImpl` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneConfigurationImpl.kt` |
| `ControlPlaneDirectoryRemoteSource` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `ControlPlaneRequestRejectedException` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `ControlPlaneRequestRouter` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestRouter.kt` |
| `ControlPlaneUnavailableException` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneRequestExceptions.kt` |
| `DirectoryEnvelope` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPayload` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `DirectoryPlane` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `HttpControlPlaneDirectorySynchronizer` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneDirectorySynchronizer.kt` |
| `HttpControlPlaneHealthMonitor` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/HttpControlPlaneHealthMonitor.kt` |
| `HttpNodeControlPlaneDirectorySource` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectory` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDirectorySource` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDirectorySource.kt` |
| `NodeControlPlaneDiscoverySynchronizer` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/NodeControlPlaneDiscoverySynchronizer.kt` |
| `SignedControlPlaneDirectoryVerifier` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/SignedControlPlaneDirectory.kt` |
| `SignedDirectoryControlPlaneCandidateVerifier` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneCandidateVerifier.kt` |
| `SignedDirectoryDownload` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/ControlPlaneDirectoryRemoteSource.kt` |
| `VerifiedControlPlaneDirectoryState` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/controlplane/VerifiedControlPlaneDirectoryState.kt` |
| `CachedNodeDirectory` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `DataStoreNodeDirectoryCache` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DataStoreNodeDirectoryCache.kt` |
| `DefaultNodeEndpointResolver` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/DefaultNodeEndpointResolver.kt` |
| `FailedNodeTracker` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/FailedNodeTracker.kt` |
| `HttpNodeDirectorySource` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeCapability` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeDirectory` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeDirectoryCache` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryCache.kt` |
| `NodeDirectorySource` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectorySource.kt` |
| `NodeDirectoryVerifier` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryVerifier.kt` |
| `NodeEndpoint` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `NodeEndpointResolver` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointResolver.kt` |
| `NodeEndpointSelector` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeEndpointSelector.kt` |
| `RegistryAuthorityCertificate` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `RegistrySigningCertificate` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SignedNodeDirectory` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `SparrowNodeDescriptor` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedNodeDescriptor` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistryAuthorityCertificate` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `UnsignedRegistrySigningCertificate` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/discovery/NodeDirectoryModels.kt` |
| `ClientRoute` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `ClientRouteRegistration` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `FederatedEnvelope` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/FederatedEnvelope.kt` |
| `GatewayBlobUploadTicket` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `GatewayBlobUploadTicketRequest` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayBlobUploadTicket.kt` |
| `GatewayClientMessage` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayClientMessage.kt` |
| `GatewayEnvelopeAcceptance` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayEnvelopeAcceptance.kt` |
| `GatewayIndicatorEvent` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayIndicatorEvent.kt` |
| `GatewayNodeInformation` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `GatewayServerMessage` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/GatewayServerMessage.kt` |
| `TransportEnvelope` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/TransportEnvelope.kt` |
| `UnsignedClientRoute` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/gateway/model/ClientRouteRegistration.kt` |
| `CreateMailboxRequest` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `CreateMailboxResponse` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `HttpMailboxGateway` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/HttpMailboxGateway.kt` |
| `MailboxEnvelopesResponse` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `MailboxGateway` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/mailbox/MailboxGateway.kt` |
| `ClientPresenceRouteCoordinator` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `ClientPresenceRouteEffect` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteEvent` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteSession` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `ClientPresenceRouteState` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteStateMachine` | `object` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientPresenceRouteTransition` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteStateMachine.kt` |
| `ClientRouteRegistrationFactory` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientRouteRegistrationFactory.kt` |
| `PresenceRouteConnection` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/ClientPresenceRouteCoordinator.kt` |
| `PresenceRouteRefreshRejectedException` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/presence/PresenceRouteRefreshRejectedException.kt` |
| `HttpPushTokenRegistrationGateway` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/HttpPushTokenRegistrationGateway.kt` |
| `PushDeviceRegistrationRequest` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushModels.kt` |
| `PushPlatform` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `PushTokenRegistrationGateway` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/PushTokenRegistrationGateway.kt` |
| `HttpPendingEnvelopeGateway` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/HttpPendingEnvelopeGateway.kt` |
| `PendingEnvelopeGateway` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeGateway.kt` |
| `PendingTransportEnvelopesResponse` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/push/inbox/PendingEnvelopeModels.kt` |
| `DefaultLocalBootstrapRoutingIdProvider` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalBootstrapRoutingIdProvider.kt` |
| `DefaultLocalRoutingIdProvider` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/DefaultLocalRoutingIdProvider.kt` |
| `LocalBootstrapRoutingIdProvider` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalBootstrapRoutingIdProvider.kt` |
| `LocalRoutingIdProvider` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/LocalRoutingIdProvider.kt` |
| `RoutingIdGenerator` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/RoutingIdGenerator.kt` |
| `Sha256RoutingIdGenerator` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/routing/Sha256RoutingIdGenerator.kt` |
| `WebSocketOutgoingWireSender` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/sender/WebSocketOutgoingWireSender.kt` |
| `DefaultWebSocketTransportClient` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/DefaultWebSocketTransportClient.kt` |
| `GatewayPendingRequestRegistry` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayPendingRequestRegistry.kt` |
| `GatewayServerMessageHandler` | `class` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/GatewayServerMessageHandler.kt` |
| `WebSocketTransportClient` | `interface` | `commonMain` | `feature/transport/src/commonMain/kotlin/com/cbgm/sparrow/feature/transport/websocket/WebSocketTransportClient.kt` |
