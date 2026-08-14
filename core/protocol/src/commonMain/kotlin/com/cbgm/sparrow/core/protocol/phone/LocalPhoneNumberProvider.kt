package com.cbgm.sparrow.core.protocol.phone

interface LocalPhoneNumberProvider {
    suspend fun getLocalPhoneNumber(): Result<String>
}
