package com.cbgm.sparrow.core.protocol.di

import com.cbgm.sparrow.core.protocol.authorization.DirectChatAuthorizationRevocationProtocol
import com.cbgm.sparrow.core.protocol.authorization.DirectChatAuthorizationRevocationSender
import com.cbgm.sparrow.core.protocol.codec.KotlinxPacketCodec
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import com.cbgm.sparrow.core.protocol.handler.DefaultProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationDeclineProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationHandshakeProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationPayloadEncoder
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayloadCodec
import com.cbgm.sparrow.core.protocol.message.MessageEditPayloadCodec
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val protocolModule =
    module {
        single { GroupProtocolPayloadEncoder() }
        single { ContactInvitationPayloadEncoder() }
        single { ContactInvitationDeclineProtocol(signatureCrypto = get(), payloadEncoder = get()) }
        single { ContactInvitationRequestProtocol(signatureCrypto = get(), payloadEncoder = get()) }
        single { ContactInvitationHandshakeProtocol(signatureCrypto = get(), payloadEncoder = get()) }
        single { DirectChatAuthorizationRevocationProtocol(signatureCrypto = get()) }
        single {
            DirectChatAuthorizationRevocationSender(
                protocol = get(),
                signingKeyPairProvider = get(),
                protocolOutbox = get()
            )
        }

        single<Json> {
            createProtocolJson()
        }

        single {
            GroupMessageContentCodec(json = get())
        }

        single {
            MessageDeletionPayloadCodec(json = get())
        }

        single {
            MessageEditPayloadCodec(json = get())
        }

        single<PacketCodec> {
            KotlinxPacketCodec(json = get())
        }

        single<ProtocolPacketHandler> {
            DefaultProtocolPacketHandler(handlers = getAll<TypedProtocolPacketHandler>())
        }

        single<PhoneNumberNormalizer> {
            DefaultPhoneNumberNormalizer()
        }
    }
