package com.cbgm.sparrow.feature.identity.device

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AndroidIdentityBackupCodecTest {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun encryptedFileRestoresExactOriginalKeysAndDoesNotExposeThem(): Unit = runBlocking {
        SodiumRuntime.initialize().getOrThrow()
        val pair = SodiumIdentityKeyGenerator().generate().getOrThrow()
        val backup = IdentityBackup(
            PublicIdentity(pair.encryptionPublicKey.toByteArray(), pair.signingPublicKey.toByteArray()),
            pair.encryptionPrivateKey.toByteArray(), pair.signingPrivateKey.toByteArray()
        )
        val codec = AndroidIdentityBackupCodec()
        val file = codec.encrypt(backup, "long-test-password-123".toCharArray())
        assertFalse(file.toString(Charsets.UTF_8).contains(backup.signingPrivateKey.joinToString(",")))
        val restored = codec.decrypt(file, "long-test-password-123".toCharArray())
        assertContentEquals(backup.publicIdentity.encryptionPublicKey, restored.publicIdentity.encryptionPublicKey)
        assertContentEquals(backup.publicIdentity.signingPublicKey, restored.publicIdentity.signingPublicKey)
        assertContentEquals(backup.encryptionPrivateKey, restored.encryptionPrivateKey)
        assertContentEquals(backup.signingPrivateKey, restored.signingPrivateKey)
        assertFailsWith<IllegalArgumentException> {
            codec.decrypt(file, "incorrect-password".toCharArray())
        }
        val json = org.json.JSONObject(file.toString(Charsets.UTF_8))
        val ciphertext = json.getString("ciphertext")
        json.put("ciphertext", (if (ciphertext[0] == 'A') "B" else "A") + ciphertext.drop(1))
        assertFailsWith<IllegalArgumentException> {
            codec.decrypt(json.toString().toByteArray(), "long-test-password-123".toCharArray())
        }
        assertFailsWith<IllegalArgumentException> {
            codec.decrypt(ByteArray(16_385), "long-test-password-123".toCharArray())
        }
    }
}
