package com.cbgm.sparrow.data.datastore.expense

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Expense receipt images, one folder per group. Names are hashed so they never leak IDs. */
class ExpenseReceiptFileStore(
    rootDirectory: String,
    private val cryptoHash: CryptoHash,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val directory: Path = rootDirectory.toPath() / EXPENSE_RECEIPT_DIRECTORY

    init {
        fileSystem.createDirectories(directory)
    }

    fun pathOf(key: ExpenseReceiptKey): String = receiptPath(key).toString()

    fun exists(key: ExpenseReceiptKey): Boolean = fileSystem.exists(receiptPath(key))

    fun read(key: ExpenseReceiptKey): ByteArray? {
        val path = receiptPath(key)
        return if (fileSystem.exists(path)) fileSystem.read(path) { readByteArray() } else null
    }

    fun write(key: ExpenseReceiptKey, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Expense receipt must not be empty" }
        val target = receiptPath(key)
        fileSystem.createDirectories(groupDirectory(key.groupId))
        val temporary = target.parent!! / "${target.name}.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.delete(target, mustExist = false)
        fileSystem.atomicMove(temporary, target)
    }

    fun importFile(key: ExpenseReceiptKey, sourcePath: String) {
        val source = sourcePath.toPath()
        val target = receiptPath(key)
        fileSystem.createDirectories(groupDirectory(key.groupId))
        fileSystem.delete(target, mustExist = false)
        runCatching { fileSystem.atomicMove(source, target) }
            .onFailure {
                write(key, fileSystem.read(source) { readByteArray() })
                fileSystem.delete(source, mustExist = false)
            }
    }

    fun delete(key: ExpenseReceiptKey) {
        fileSystem.delete(receiptPath(key), mustExist = false)
    }

    fun deleteGroup(groupId: String) {
        fileSystem.deleteRecursively(groupDirectory(groupId), mustExist = false)
    }

    private fun groupDirectory(groupId: String): Path = directory / hash(groupId)

    private fun receiptPath(key: ExpenseReceiptKey): Path =
        groupDirectory(key.groupId) / "${hash(key.receiptId)}.jpg"

    private fun hash(value: String): String {
        require(value.isNotBlank()) { "Expense receipt key must not be blank" }
        return cryptoHash
            .sha256(value.encodeToByteArray())
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0')
            }
    }

    private companion object {
        const val EXPENSE_RECEIPT_DIRECTORY = "expense-receipts"
    }
}

data class ExpenseReceiptKey(
    val groupId: String,
    val receiptId: String
)
