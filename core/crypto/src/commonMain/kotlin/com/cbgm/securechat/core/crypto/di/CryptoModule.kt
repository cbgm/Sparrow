package com.cbgm.securechat.core.crypto.di

import com.cbgm.securechat.core.crypto.InitializeCryptoRuntime
import com.cbgm.securechat.core.crypto.group.GroupCrypto
import com.cbgm.securechat.core.crypto.group.SodiumGroupCrypto
import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementPayloadEncoder
import com.cbgm.securechat.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.securechat.core.crypto.identity.SodiumIdentityAcknowledgementCrypto
import com.cbgm.securechat.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.securechat.core.crypto.random.SecureRandomGenerator
import com.cbgm.securechat.core.crypto.random.SodiumSecureRandomGenerator
import com.cbgm.securechat.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.crypto.signature.SodiumDetachedSignatureCrypto
import com.cbgm.securechat.core.crypto.transport.DefaultIncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.DefaultTransportPayloadCodec
import com.cbgm.securechat.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.securechat.core.crypto.transport.SodiumTransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import org.koin.dsl.module

val cryptoModule =
    module {
        single {
            InitializeCryptoRuntime()
        }

        single<CryptoHash> {
            DefaultCryptoHash()
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
