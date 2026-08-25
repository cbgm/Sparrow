package com.cbgm.sparrow.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

fun createSparrowDataStore(filePath: String): SparrowDataStore =
    SparrowDataStore(
        dataStore =
            PreferenceDataStoreFactory.createWithPath {
                filePath.toPath()
            }
    )

const val SPARROW_DATA_STORE_FILE_NAME = "sparrow.preferences_pb"
