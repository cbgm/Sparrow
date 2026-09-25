package com.cbgm.sparrow.feature.identity.device

import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup

/** Portable password encryption. Android implementation uses PBKDF2-HMAC-SHA256 and AES-256-GCM. */
interface IdentityBackupCodec {
    fun encrypt(backup: IdentityBackup, password: CharArray): ByteArray

    fun decrypt(document: ByteArray, password: CharArray): IdentityBackup
}
