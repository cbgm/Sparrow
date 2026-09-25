package com.cbgm.sparrow.data.database.migration

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable(
    fromTableName = "identity_invitations",
    toTableName = "identity_exchanges"
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "identity_invitations",
        fromColumnName = "invitationId",
        toColumnName = "exchangeId"
    ),
    RenameColumn(
        tableName = "identity_invitations",
        fromColumnName = "state",
        toColumnName = "stage"
    )
)
@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "identity_invitations",
        columnName = "viewedAtEpochMilliseconds"
    ),
    DeleteColumn(
        tableName = "identity_invitations",
        columnName = "hiddenAtEpochMilliseconds"
    ),
    DeleteColumn(
        tableName = "identity_invitations",
        columnName = "resultAction"
    )
)
class IdentityExchangeMigration41To42 : AutoMigrationSpec
