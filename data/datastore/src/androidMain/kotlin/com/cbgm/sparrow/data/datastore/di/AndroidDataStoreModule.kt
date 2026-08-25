package com.cbgm.sparrow.data.datastore.di

import com.cbgm.sparrow.data.datastore.SPARROW_DATA_STORE_FILE_NAME
import com.cbgm.sparrow.data.datastore.createSparrowDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDataStoreModule =
    module {
        single {
            createSparrowDataStore(
                filePath = androidContext().filesDir.resolve(SPARROW_DATA_STORE_FILE_NAME).absolutePath
            )
        }
    }
