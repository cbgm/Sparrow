# Contacts

`:feature:contacts` owns contact records, phone numbers, contact-to-routing mappings, blocking, device-contact integration and peer/contact identity projections. **Generic invitation lifecycle is no longer owned here**; it lives in `:feature:invite`. Identity exchange/trust lives in `:feature:identity`.

## Main repositories and datasources

- `ContactRepository` / `ContactRepositoryImpl`
- `ContactTransportRepository` / `ContactTransportRepositoryImpl`
- `IdentityPeerRepository` / `IdentityPeerRepositoryImpl`
- `ContactBlocklistRepository`
- `ContactLocalDataSource`
- `ContactRoutingDataSource`
- `ContactRoutingIdDataSource`
- `ContactRoutingReconciliationDataSource`
- `ContactByRoutingIdDataSource`
- `MailboxContactDataSource`

## Important models

- `Contact`
- `ContactPhoneNumber`, `ContactPhoneNumberType`
- `IncomingPeerContactCandidate`
- `IdentityImportTrust`
- `SparrowIdentity`
- `IdentityPeerResolution`
- `MailboxContactState`
- `DeviceContact`, `DevicePhoneNumber`

## Contact/peer resolution

Cross-feature orchestration uses contact domain use cases such as:

- `ResolveIncomingPeerContactsUseCase`
- `ResolveContactInvitationRoutingIdUseCase`
- `ResolveContactTransportRoutingIdUseCase`
- `ResolveContactIdByRoutingIdUseCase`
- `ResolveContactBootstrapRoutingIdUseCase`
- `ApplyIdentityPeerMergeUseCase`
- `UpdateIncomingIdentityPeerMetadataUseCase`
- `GetIdentityPeerDisplayNameUseCase`

This keeps contact merging/routing ownership inside Contacts while `ConversationFlowHandler` decides *when* those operations are part of an invite/identity/membership workflow.

## Device contacts

Device-contact reading/writing is exposed through repository/use-case boundaries including `ImportDeviceContactsUseCase`, `AddDeviceContactUseCase`, `DeviceContactsRepository`, `DeviceContactWriterRepository` and permission repositories/adapters.

`AppViewModel` performs device contact synchronization after local identity readiness when read-contact permission is available.

## Blocking

- `BlockContactUseCase`
- `UnblockContactUseCase`
- `ObserveContactBlocklistUseCase`
- `ObserveBlockedContactsContextUseCase`
- `BlockedContactsViewModel`

`ContactBlockObserver` in `:feature:conversationorchestration` reacts to persisted block state and asks `ConversationFlowHandler` to revoke the peer exchange as needed. Contacts does not call Identity repositories directly to perform the cross-feature revocation workflow.

## Invitation relationship

Contact selection can initiate an invitation, but the invitation row/result lifecycle is owned by `:feature:invite`. See [Invitations](invitations.md).

## Identity relationship

Contacts stores contact-facing peer metadata/public identity linkage; the identity-exchange state machine and remote replacement/recovery lifecycle are owned by `:feature:identity`. See [Identity](identity.md) and [Identity recovery](identity-recovery.md).
