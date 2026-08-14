package com.cbgm.securechat.feature.identity.di

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.securechat.feature.identity.data.datasource.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.datasource.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.identity.data.datasource.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.datasource.PublicIdentityStorage
import com.cbgm.securechat.feature.identity.data.repository.AndroidLocalIdentityProfileRepositoryImpl
import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository
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

private const val IDENTITY_PREFERENCES_NAME = "secure_chat_identity"
