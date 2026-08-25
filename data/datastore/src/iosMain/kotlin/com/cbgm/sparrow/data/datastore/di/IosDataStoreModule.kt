package com.cbgm.sparrow.core.datastore.di

import com.cbgm.sparrow.core.datastore.SPARROW_DATA_STORE_FILE_NAME
import com.cbgm.sparrow.core.datastore.createSparrowDataStore
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

val iosDataStoreModule =
    module {
        single {
            val directory =
                requireNotNull(
                    NSFileManager.defaultManager.URLForDirectory(
                        directory = NSApplicationSupportDirectory,
                        inDomain = NSUserDomainMask,
                        appropriateForURL = null,
                        create = true,
                        error = null
                    )?.path
                )
            createSparrowDataStore(filePath = "$directory/$SPARROW_DATA_STORE_FILE_NAME")
        }
    }
