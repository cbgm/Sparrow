package com.cbgm.sparrow.feature.payment.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AndroidBankDetailsStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun file(key: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return File(context.noBackupFilesDir, "payment-bank-details/$hash.bin")
    }

    @Test fun encryptedDetailsSurviveRecreationAndCanBeDeleted() = runTest {
        val key = UUID.randomUUID().toString()
        val value = "{\"accountHolder\":\"Test Recipient\",\"iban\":\"DE89370400440532013000\"}"
        val first = AndroidBankDetailsStorage(context)
        try {
            first.write(key, value)
            assertFalse(file(key).readBytes().toString(Charsets.ISO_8859_1).contains("DE89370400440532013000"))
            assertEquals(value, AndroidBankDetailsStorage(context).read(key))
            first.delete(key)
            assertNull(first.read(key))
        } finally {
            first.delete(key)
        }
    }

    @Test fun authenticationRejectsTamperedCiphertext() = runTest {
        val key = UUID.randomUUID().toString()
        val storage = AndroidBankDetailsStorage(context)
        try {
            storage.write(key, "secret bank details")
            val bytes = file(key).readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
            file(key).writeBytes(bytes)
            assertFails { storage.read(key) }
        } finally {
            storage.delete(key)
        }
    }

    @Test fun ciphertextCannotBeMovedToAnotherRecipient() = runTest {
        val key = UUID.randomUUID().toString()
        val other = UUID.randomUUID().toString()
        val storage = AndroidBankDetailsStorage(context)
        try {
            storage.write(key, "secret bank details")
            file(key).copyTo(file(other), overwrite = true)
            assertFails { storage.read(other) }
        } finally {
            storage.delete(key)
            storage.delete(other)
        }
    }
}
