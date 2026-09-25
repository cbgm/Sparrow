# `:feature:contacts`

Source directory: `feature/contacts`

## Direct project dependencies

- `:core`
- `:core:crypto`
- `:core:protocol`
- `:core:ui`
- `:feature:avatar`
- `:feature:identity`
- `:feature:transport`
- `:data:database`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidDeviceContactWriterRepository` | `class` | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactWriterRepository.kt` |
| `AndroidDeviceContactsRepository` | `class` | `androidMain` | `feature/contacts/src/androidMain/kotlin/com/cbgm/sparrow/feature/contacts/device/AndroidDeviceContactsRepository.kt` |
| `ContactByRoutingIdDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactByRoutingIdDataSource.kt` |
| `ContactLocalDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactLocalDataSource.kt` |
| `ContactRoutingDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingDataSource.kt` |
| `ContactRoutingIdDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingIdDataSource.kt` |
| `ContactRoutingReconciliationDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/ContactRoutingReconciliationDataSource.kt` |
| `MailboxContactDataSource` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `MailboxContactStateDto` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/datasource/MailboxContactDataSource.kt` |
| `ContactRepositoryImpl` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactRepositoryImpl.kt` |
| `ContactTransportRepositoryImpl` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/ContactTransportRepositoryImpl.kt` |
| `IdentityPeerRepositoryImpl` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/data/repository/IdentityPeerRepositoryImpl.kt` |
| `BlockedContactsContext` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/BlockedContactsContext.kt` |
| `Contact` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/Contact.kt` |
| `ContactBlocklist` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactBlocklist.kt` |
| `ContactDetailsContext` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactDetailsContext.kt` |
| `ContactPhoneNumber` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumber.kt` |
| `ContactPhoneNumberType` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactPhoneNumberType.kt` |
| `ContactsWithProfilePictures` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ContactsWithProfilePictures.kt` |
| `DeviceContactLinkStatus` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/DeviceContactLinkStatus.kt` |
| `IdentityImportTrust` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IdentityImportTrust.kt` |
| `ImportContactRequest` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportContactRequest.kt` |
| `ImportDeviceContactRequest` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDeviceContactRequest.kt` |
| `ImportDevicePhoneNumber` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/ImportDevicePhoneNumber.kt` |
| `IncomingPeerContactCandidate` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/IncomingPeerContactCandidate.kt` |
| `MailboxContactState` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/MailboxContactState.kt` |
| `SparrowIdentity` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/SparrowIdentity.kt` |
| `AddDeviceContactRequest` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `AddDeviceContactResult` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/AddDeviceContact.kt` |
| `DeviceContact` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DeviceContact.kt` |
| `DevicePhoneNumber` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumber.kt` |
| `DevicePhoneNumberType` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/device/DevicePhoneNumberType.kt` |
| `IdentityPeerMerge` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `IdentityPeerResolution` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/model/identity/IdentityPeerResolution.kt` |
| `ContactBlocklistRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactBlocklistRepository.kt` |
| `ContactRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactRepository.kt` |
| `ContactTransportRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/ContactTransportRepository.kt` |
| `DeviceContactWriterRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactWriterRepository.kt` |
| `DeviceContactsRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/DeviceContactsRepository.kt` |
| `IdentityPeerRepository` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/repository/IdentityPeerRepository.kt` |
| `AddDeviceContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/AddDeviceContactUseCase.kt` |
| `BlockContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/BlockContactUseCase.kt` |
| `GetContactSafetyNumberUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactSafetyNumberUseCase.kt` |
| `GetContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetContactUseCase.kt` |
| `GetMailboxContactStatesUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMailboxContactStatesUseCase.kt` |
| `GetMutualContactSigningPublicKeyUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/GetMutualContactSigningPublicKeyUseCase.kt` |
| `ImportContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportContactUseCase.kt` |
| `ImportDeviceContactsUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ImportDeviceContactsUseCase.kt` |
| `ObserveBlockedContactsContextUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveBlockedContactsContextUseCase.kt` |
| `ObserveContactBlocklistUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactBlocklistUseCase.kt` |
| `ObserveContactDetailsContextUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactDetailsContextUseCase.kt` |
| `ObserveContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactUseCase.kt` |
| `ObserveContactsUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveContactsUseCase.kt` |
| `ObserveIdentitySetupModeUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ObserveIdentitySetupModeUseCase.kt` |
| `ReconcileContactTransportRoutingUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ReconcileContactTransportRoutingUseCase.kt` |
| `ResolveContactBootstrapRoutingIdUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactBootstrapRoutingIdUseCase.kt` |
| `ResolveContactIdByRoutingIdUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactIdByRoutingIdUseCase.kt` |
| `ResolveContactInvitationRoutingIdUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactInvitationRoutingIdUseCase.kt` |
| `ResolveContactTransportRoutingIdUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveContactTransportRoutingIdUseCase.kt` |
| `ResolveIncomingPeerContactsUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/ResolveIncomingPeerContactsUseCase.kt` |
| `UnblockContactUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/UnblockContactUseCase.kt` |
| `ApplyIdentityPeerMergeUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/ApplyIdentityPeerMergeUseCase.kt` |
| `GetIdentityPeerDisplayNameUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/GetIdentityPeerDisplayNameUseCase.kt` |
| `InspectContactPeerUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/InspectContactPeerUseCase.kt` |
| `UpdateIncomingIdentityPeerMetadataUseCase` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/domain/usecase/identity/UpdateIncomingIdentityPeerMetadataUseCase.kt` |
| `BlockedContactsViewModel` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/BlockedContactsViewModel.kt` |
| `BlockedContactsEffect` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsEffect.kt` |
| `BlockedContactsUiEvent` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiEvent.kt` |
| `BlockedContactsUiState` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/blocklist/model/BlockedContactsUiState.kt` |
| `ContactDetailsContent` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsRoute.kt` |
| `ContactDetailsViewModel` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/ContactDetailsViewModel.kt` |
| `ContactDetailPage` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsContent.kt` |
| `ContactDetailsPreviewData` | `object` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/components/ContactDetailsPreviewData.kt` |
| `ContactDetailsContactUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactDetailsUiEvent` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiEvent.kt` |
| `ContactDetailsUiState` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsUiState.kt` |
| `ContactPhoneNumberTypeUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactPhoneNumberUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `DeviceContactLinkUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `SparrowIdentityUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/details/model/ContactDetailsContactUi.kt` |
| `ContactsListMode` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsScreen.kt` |
| `ContactsViewModel` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/ContactsViewModel.kt` |
| `ContactGroupEntity` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactGroupEntity.kt` |
| `ContactUi` | `class` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactUi.kt` |
| `ContactsEffect` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsEffect.kt` |
| `ContactsScreenMode` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsScreenMode.kt` |
| `ContactsUiEvent` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiEvent.kt` |
| `ContactsUiState` | `interface` | `commonMain` | `feature/contacts/src/commonMain/kotlin/com/cbgm/sparrow/feature/contacts/presentation/overview/model/ContactsUiState.kt` |
