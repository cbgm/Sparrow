# Identity

`:feature:identity` owns the local SecureChat identity, its storage ports, identity-sharing codec,
and setup/share presentation. Remote contact identities and verification belong to
`:feature:contacts`.

## Package structure

```text
feature/identity/.../feature/identity/
├── domain/
│   ├── model/              # PublicIdentity, IdentityStatus, shared payload
│   ├── repository/
│   │   ├── IdentityRepository.kt
│   │   └── storage/        # private/public key and phone-name ports
│   ├── service/            # IdentityShareCodec
│   └── usecase/            # create, inspect, normalize, save, share
├── data/
│   ├── protocol/           # adapters implementing core protocol identity ports
│   ├── repository/         # DefaultIdentityRepository
│   └── sharing/            # DefaultIdentityShareCodec
├── presentation/
│   ├── model/
│   ├── platform/           # share, phone hint, QR abstractions
│   └── screen/
│       ├── setup/component/
│       └── share/
└── di/IdentityModule.kt
```

There is no startup package inside identity. Startup UI and initialization are in the separate
`:startup` module. Android process runtime startup is in `SecureChatApplication`.

## Local identity domain

`IdentityRepository` exposes local identity lifecycle. `DefaultIdentityRepository` coordinates key
generation and the storage ports:

- `PrivateKeyStorage`;
- `PublicIdentityStorage`.

`LocalPhoneNameStorage` stores the local phone/name data used by identity setup and gateway-address
derivation.

Main use cases:

| Use case | Responsibility |
|---|---|
| `CreateIdentity` | Generate and persist a local identity |
| `GetIdentityStatus` | Determine setup state |
| `GetPublicIdentity` | Return public encryption/signing material |
| `GetLocalPhoneNumber` | Read the configured phone number |
| `NormalizeLocalPhoneNumber` | Normalize input through `PhoneNumberNormalizer` |
| `SaveLocalPhoneName` | Persist phone/name data |
| `CreateSharedIdentity` | Build an encoded share payload |

## Protocol adapters

Other modules depend on stable interfaces in `:core:protocol`, not on `IdentityRepository`
directly. Identity supplies these adapters:

| Core protocol port | Identity adapter |
|---|---|
| `LocalEncryptionKeyPairProvider` | `IdentityLocalEncryptionKeyPairProvider` |
| `LocalPublicIdentityProvider` | `IdentityLocalPublicIdentityProvider` |
| `LocalSigningKeyPairProvider` | `IdentityLocalSigningKeyPairProvider` |
| `LocalSigningPublicKeyProvider` | `IdentityLocalSigningPublicKeyProvider` |
| `LocalPhoneNumberProvider` | `IdentityLocalPhoneNumberProvider` |

This keeps `:core:protocol`, `:feature:messaging`, and `:feature:contacts` independent of identity
storage details.

## Setup and sharing UI

`IdentityRoute` renders `IdentityScreen` with `IdentityViewModel`.
`ShareIdentityRoute` renders `ShareIdentityScreen` with `ShareIdentityViewModel`.
Setup-specific reusable elements are under `presentation/screen/setup/component`.

Platform actions are abstracted under `presentation/platform`, including
`rememberIdentityShareLauncher()`, `PhoneNumberHintLauncher()`, and QR-code support.

`DefaultIdentityShareCodec` handles the share representation. Sharing public identity data is
separate from the gateway-based identity exchange.

## Messaging integration

Once identity and phone number are ready, `SecureChatApplication` starts the gateway runtime.

- `DefaultLocalRoutingIdProvider` reads the phone through `LocalPhoneNumberProvider`.
- `DefaultIncomingEnvelopeRunner` obtains decryption keys through
  `LocalEncryptionKeyPairProvider`.
- `DefaultIdentityExchangeStarter` obtains public keys through `LocalPublicIdentityProvider`.
- Identity packet handlers obtain signing material through `LocalSigningKeyPairProvider`.

The identity feature does not send WebSocket frames and does not own remote contact trust.

## Extension rules

- Keep private key access behind storage/provider interfaces.
- Add local identity operations as use cases.
- Keep remote identity and safety-number verification in `:feature:contacts`.
- Implement protocol-facing needs as adapters to `:core:protocol` ports.
- Keep startup flow in `:startup`/`:androidApp`, not under the identity package.
- Keep platform sharing and hints behind presentation platform abstractions.
