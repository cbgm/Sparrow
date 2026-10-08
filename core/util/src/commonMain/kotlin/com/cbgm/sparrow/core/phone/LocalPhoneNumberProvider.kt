package com.cbgm.sparrow.core.phone

interface LocalPhoneNumberProvider {
    suspend fun getLocalPhoneNumber(): Result<String>
}
