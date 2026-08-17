package com.cbgm.sparrow.feature.identity.di

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.sparrow.feature.identity.data.datasource.AndroidPrivateKeyStorage
import com.cbgm.sparrow.feature.identity.data.datasource.AndroidPublicIdentityStorage
import com.cbgm.sparrow.feature.identity.data.datasource.PrivateKeyStorage
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityStorage
import com.cbgm.sparrow.feature.identity.data.repository.AndroidLocalIdentityProfileRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.AndroidLocalProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidIdentityStorageModule =
    module {
        single<SharedPreferences> {
            androidContext().getSharedPreferences(
                IDENTITY_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        }

        single<LocalIdentityProfileRepository> {
            AndroidLocalIdentityProfileRepositoryImpl(
                preferences = get()
            )
        }

        single<LocalProfilePictureRepository> {
            AndroidLocalProfilePictureRepositoryImpl(
                context = androidContext(),
                preferences = get()
            )
        }

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(
                context = androidContext()
            )
        }

        single<PublicIdentityStorage> {
            AndroidPublicIdentityStorage(
                context = androidContext()
            )
        }
    }

private const val IDENTITY_PREFERENCES_NAME = "sparrow_identity"
