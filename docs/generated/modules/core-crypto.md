# `:core:crypto`

Source directory: `core/crypto`

## Direct project dependencies

None detected.

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `InitializeCryptoRuntime` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/InitializeCryptoRuntime.kt` |
| `SodiumRuntime` | `object` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/SodiumRuntime.kt` |
| `BlobCipher` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `EncryptedBlob` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/BlobCipher.kt` |
| `SodiumBlobCipher` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/blob/SodiumBlobCipher.kt` |
| `CryptoException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `CryptoNotInitializedException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPrivateKeyException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `InvalidPublicKeyException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageDecryptionException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `MessageEncryptionException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `SignatureVerificationException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/SignatureVerificationException.kt` |
| `UnsupportedCryptoVersionException` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/error/CryptoException.kt` |
| `GroupCiphertext` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupCiphertext.kt` |
| `GroupCrypto` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupCrypto.kt` |
| `GroupKeyConfirmation` | `object` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupKeyConfirmation.kt` |
| `GroupKeyStore` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/GroupKeyStore.kt` |
| `SodiumGroupCrypto` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/group/SodiumGroupCrypto.kt` |
| `CryptoHash` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/CryptoHash.kt` |
| `DefaultCryptoHash` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/hash/DefaultCryptoHash.kt` |
| `IdentityAcknowledgementCrypto` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityAcknowledgementCrypto.kt` |
| `IdentityAcknowledgementPayloadEncoder` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityAcknowledgementPayloadEncoder.kt` |
| `IdentityKeyGenerator` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityKeyGenerator.kt` |
| `IdentityKeyPair` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/IdentityKeyPair.kt` |
| `SodiumIdentityAcknowledgementCrypto` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/SodiumIdentityAcknowledgementCrypto.kt` |
| `SodiumIdentityKeyGenerator` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/identity/SodiumIdentityKeyGenerator.kt` |
| `PublicIdentityKeySet` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/model/PublicIdentityKeySet.kt` |
| `SecureRandomGenerator` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SecureRandomGenerator.kt` |
| `SodiumSecureRandomGenerator` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/random/SodiumSecureRandomGenerator.kt` |
| `SafetyNumber` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/safety/SafteyNumber.kt` |
| `SafetyNumberGenerator` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/safety/SafetyNumberGenerator.kt` |
| `DetachedSignatureCrypto` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/DetachedSignatureCrypto.kt` |
| `SodiumDetachedSignatureCrypto` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/signature/SodiumDetachedSignatureCrypto.kt` |
| `DecodedTransportMessage` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DecodedTransportMessage.kt` |
| `DefaultIncomingTransportMessageDecoder` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultIncomingTransportMessageDecoder.kt` |
| `DefaultTransportPayloadCodec` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/DefaultTransportPayloadCodec.kt` |
| `EncryptedTransportPayload` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/EncryptedTransportPayload.kt` |
| `IncomingTransportMessageDecoder` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/IncomingTransportMessageDecoder.kt` |
| `SodiumTransportMessageCipher` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/SodiumTransportMessageCipher.kt` |
| `TransportEncryptionMode` | `class` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportEncryptionMode.kt` |
| `TransportMessageCipher` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportMessageCipher.kt` |
| `TransportPayloadCodec` | `interface` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/transport/TransportPayloadCodec.kt` |
| `ByteArrays` | `object` | `commonMain` | `core/crypto/src/commonMain/kotlin/com/cbgm/sparrow/core/crypto/util/ByteArrays.kt` |
