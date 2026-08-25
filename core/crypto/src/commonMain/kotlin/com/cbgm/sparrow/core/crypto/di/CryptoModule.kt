package com.cbgm.sparrow.core.crypto.di

import com.cbgm.sparrow.core.crypto.InitializeCryptoRuntime
import com.cbgm.sparrow.core.crypto.blob.BlobCipher
import com.cbgm.sparrow.core.crypto.blob.SodiumBlobCipher
import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.crypto.group.SodiumGroupCrypto
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.crypto.identity.IdentityAcknowledgementPayloadEncoder
import com.cbgm.sparrow.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityAcknowledgementCrypto
import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.crypto.random.SodiumSecureRandomGenerator
import com.cbgm.sparrow.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.signature.SodiumDetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.DefaultIncomingTransportMessageDecoder
import com.cbgm.sparrow.core.crypto.transport.DefaultTransportPayloadCodec
import com.cbgm.sparrow.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.sparrow.core.crypto.transport.SodiumTransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import org.koin.dsl.module

val cryptoModule =
    module {
        single {
            InitializeCryptoRuntime()
        }

        single<CryptoHash> {
            DefaultCryptoHash()
        }

        single<BlobCipher> {
            SodiumBlobCipher()
        }

        single {
            SafetyNumberGenerator(
                cryptoHash = get()
            )
        }

        single {
            IdentityAcknowledgementPayloadEncoder()
        }

        single<IdentityAcknowledgementCrypto> {
            SodiumIdentityAcknowledgementCrypto(
                payloadEncoder = get<IdentityAcknowledgementPayloadEncoder>()
            )
        }

        single<IdentityKeyGenerator> {
            SodiumIdentityKeyGenerator()
        }

        single<SecureRandomGenerator> {
            SodiumSecureRandomGenerator()
        }

        single<DetachedSignatureCrypto> {
            SodiumDetachedSignatureCrypto()
        }

        single<GroupCrypto> {
            SodiumGroupCrypto()
        }

        single<TransportMessageCipher> {
            SodiumTransportMessageCipher()
        }

        single<TransportPayloadCodec> {
            DefaultTransportPayloadCodec()
        }

        single<IncomingTransportMessageDecoder> {
            DefaultIncomingTransportMessageDecoder(
                payloadCodec = get(),
                transportCipher = get()
            )
        }
    }
