package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.feature.identity.data.datasource.ProfilePictureFileDataSource
import com.cbgm.sparrow.feature.identity.device.AndroidPrivateKeyStorage
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidIdentityStorageModule =
    module {
        single {
            ProfilePictureFileDataSource(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(dataStore = get())
        }
    }
