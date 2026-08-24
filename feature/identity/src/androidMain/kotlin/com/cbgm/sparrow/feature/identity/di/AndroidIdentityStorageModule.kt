package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.feature.identity.data.datasource.storage.ProfilePictureFileStorage
import com.cbgm.sparrow.feature.identity.device.AndroidPrivateKeyStorage
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidIdentityStorageModule =
    module {
        single {
            ProfilePictureFileStorage(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(dataStore = get())
        }
    }
