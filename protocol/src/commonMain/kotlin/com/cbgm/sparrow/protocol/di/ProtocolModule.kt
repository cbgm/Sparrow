package com.cbgm.sparrow.protocol.di

import com.cbgm.sparrow.protocol.authorization.DirectChatAuthorizationRevocationProtocol
import com.cbgm.sparrow.protocol.authorization.DirectChatAuthorizationRevocationSender
import com.cbgm.sparrow.protocol.codec.KotlinxPacketCodec
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.codec.createProtocolJson
import com.cbgm.sparrow.protocol.handler.DefaultProtocolPacketHandler
import com.cbgm.sparrow.protocol.handler.ProtocolPacketHandler
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.invitation.ContactInvitationDeclineProtocol
import com.cbgm.sparrow.protocol.invitation.ContactInvitationHandshakeProtocol
import com.cbgm.sparrow.protocol.invitation.ContactInvitationPayloadEncoder
import com.cbgm.sparrow.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.message.MessageDeletionPayloadCodec
import com.cbgm.sparrow.protocol.message.MessageEditPayloadCodec
import com.cbgm.sparrow.protocol.packet.GroupProtocolPayloadEncoder
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
    }
