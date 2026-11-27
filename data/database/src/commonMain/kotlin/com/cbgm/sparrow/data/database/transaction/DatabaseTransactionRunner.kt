package com.cbgm.sparrow.data.database.transaction

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.cbgm.sparrow.data.database.SparrowDatabase

/** Runs writes of several DAOs as one database transaction: either all of them are committed or none. */
interface DatabaseTransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

/** DAO calls made inside [block] use the transaction's connection, because Room binds it to the coroutine. */
class RoomDatabaseTransactionRunner(
    private val database: SparrowDatabase
) : DatabaseTransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
