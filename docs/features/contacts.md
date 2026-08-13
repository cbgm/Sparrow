# Contacts

`:feature:contacts` owns the contact domain, contact persistence adapters, remote identity exchange,
verification, and reusable contacts presentation. Platform address-book access is supplied by
`:feature:contactimport`.

## Package structure

```text
feature/contacts/.../feature/contacts/
├── domain/
│   ├── device/       # platform contact ports and models
│   ├── identity/     # IdentityExchangeStarter
│   ├── model/        # Contact, phone numbers, identity and trust state
│   ├── repository/   # ContactRepository, ContactKeyExchangeStore
│   └── usecase/      # observe, import, get, verify, safety number
├── data/
│   ├── identity/     # exchange starter and local identity-change adapter
│   ├── mapper/       # database/import mapping
│   ├── merge/        # contact merge behavior
│   ├── protocol/     # identity packet handlers
│   └── repository/   # repository and key-exchange implementations
├── presentation/
│   ├── component/
│   │   ├── contactdetails/ # one previewable contact-detail component per file
│   │   └── contactlist/    # reusable contact-list components
│   ├── mapper/
│   ├── model/        # UI state, events, effects, screen mode
│   ├── platform/     # permission abstraction
│   └── screen/       # shared screen and details screen
└── di/ContactsModule.kt
```

There is no `startup` package in this feature. Application startup behavior belongs to the
`:startup` module or `SecureChatApplication`.

## Domain

`Contact` contains display information, phone numbers, device-contact linkage, and an optional
`SecureChatIdentity`. `SecureChatIdentity` includes encryption/signing public keys,
`KeyExchangeStatus`, and verification state.

Main contracts:

| Contract | Responsibility |
|---|---|
| `ContactRepository` | Observe, load, import, merge, and verify contacts |
| `ContactKeyExchangeStore` | Persist remote identity and exchange state |
| `IdentityExchangeStarter` | Ensure an identity packet is queued |
| `DeviceContactsDataSource` | Read platform contacts |
| `DeviceContactWriter` | Write or link platform contacts |

Use cases keep presentation independent of implementations: `ObserveContacts`, `ObserveContact`,
`GetContact`, `ImportContact`, `ImportDeviceContacts`, `VerifyContact`, and
`GetContactSafetyNumber`.

## One contacts screen, two modes

`ContactsScreen` is the reusable visual screen. Its variable behavior is represented by the sealed
`ContactsScreenMode`:

| Mode | Used for | Variable components |
|---|---|---|
| `ContactsScreenMode.Overview` | Normal contacts overview | `OverviewContactsTopBar`, create-group row, contact status, import FAB and sheet |
| `ContactsScreenMode.GroupSelection` | Selecting contacts for a group | `GroupSelectionContactsTopBar`, title/confirm controls, selection circles |
| `ContactsScreenMode.MemberSelection` | Adding contacts to an existing group | `MemberSelectionContactsTopBar`, confirm control, selection circles |

The shared screen always owns:

- `SecureChatLazyScaffold`;
- loading, empty, error, and content rendering;
- the grouped `LazyColumn`;
- contact-row rendering and search input plumbing.

`ContactsRoute` supplies `Overview` callbacks and obtains state from `ContactsViewModel`.
`CreateGroupScreen` in `:feature:chats` supplies `GroupSelection` and owns the group-specific
`CreateGroupViewModel`. This reuses contacts presentation without moving group creation into the
contacts domain.

`AddGroupMembersScreen` supplies `MemberSelection` from inside `GroupDetailsFlow`. It reuses the
contact list without introducing an application navigation destination or moving membership
behavior out of `:feature:chats`.

## Import and merge

`DefaultContactRepository` delegates merge decisions to `ContactMergeService`. Phone numbers are
normalized through `PhoneNumberNormalizer`, so importing a device contact can update an existing
SecureChat contact instead of intentionally creating another record.

`ContactsViewModel` combines `ObserveContacts` with `searchQuery`, then uses
`filterContacts()` and `groupContactsByInitial()` to produce `ContactsUiState`.

Platform permissions stay in presentation/platform adapters. `ContactsRoute` requests permission
through `rememberDeviceContactsPermissionRequest()` and sends results back as `ContactsEvent`.

## Remote identity exchange

`DefaultIdentityExchangeStarter` creates an `IdentityPacket` from `LocalPublicIdentityProvider` and
enqueues it through `ProtocolOutbox`.

`IdentityPacketHandler`:

1. stores the received keys through `ContactKeyExchangeStore`;
2. gets the local signing key pair;
3. signs the exact received encryption and signing keys;
4. enqueues `IdentityAcknowledgementPacket`.

`IdentityAcknowledgementPacketHandler` checks that:

- the sender key matches the contact identity already stored locally;
- acknowledged keys match the current local identity;
- the signature verifies against the stored remote signing key.

The acknowledgement proves receipt of the identity. It does not itself change trust state.

## Verification

`GetContactSafetyNumber` combines local and remote identity material through the safety-number
generator. `VerifyContact` persists the user's explicit verification decision. Replacing remote
keys must not silently retain verification; that rule belongs in `DefaultContactKeyExchangeStore`.

The manual path is:

```text
DetailsRoute
  -> ContactDetailsFlow
  -> ContactDetailsViewModel.confirmVerification()
  -> VerifyContact.invoke()
```

The QR path is:

```text
AppDestination.VerifyIdentityQr(groupId = null)
  -> VerifyIdentityQrRoute
  -> ContactQrVerificationFlow
  -> VerifyContactQrViewModel.onQrCodeScanned()
  -> VerifyContactByQr.invoke()
```

`ContactDetailsScreen` renders state only. Its detailed renderers live in
`presentation/component/contactdetails`, one component and its preview per file.

## Messaging integration

Contacts do not use WebSockets directly.

- `DefaultOutboxProcessor` loads contacts through `GetContact` to select encryption.
- `DefaultContactRoutingIdResolver` and `DefaultContactByRoutingIdResolver` live in
  `:feature:messaging`.
- Identity packet handlers use `ProtocolOutbox`.
- `ChatMessagePacketHandler` may use
  `ContactDao.usePhoneNumberAsDisplayNameWhenMissing()` for placeholder senders.

See [Conversation, Messaging, and Delivery Flow](message-transport-flow.md).

## Extension rules

- Add contact business operations as domain use cases.
- Keep Room entities and DAOs out of presentation and domain models.
- Put feature visuals under `presentation/component/<screen-name>`, one component and preview per file.
- Add behavior differences through `ContactsScreenMode` when the visual list remains the same.
- Keep group creation state in `:feature:chats`; contacts only supplies reusable selection UI.
- Keep platform address-book APIs behind device-contact ports.
- Send identity packets through `ProtocolOutbox`, never directly through transport.
