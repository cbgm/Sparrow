package com.cbgm.sparrow.core.protocol.di

import com.cbgm.sparrow.core.protocol.codec.KotlinxPacketCodec
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import com.cbgm.sparrow.core.protocol.handler.DefaultProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val protocolModule =
    module {

        single<Json> {
            createProtocolJson()
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
