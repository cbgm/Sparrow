package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.feature.identity.data.datasource.AndroidPrivateKeyStorage
import com.cbgm.sparrow.feature.identity.data.datasource.PrivateKeyStorage
import com.cbgm.sparrow.feature.identity.data.storage.ProfilePictureFileStorage
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
