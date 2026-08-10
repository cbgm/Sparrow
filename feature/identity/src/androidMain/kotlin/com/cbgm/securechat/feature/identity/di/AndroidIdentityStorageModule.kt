package com.cbgm.securechat.feature.identity.di

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.securechat.feature.identity.data.storage.AndroidLocalPhoneNameStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PublicIdentityStorage
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

        single<LocalPhoneNameStorage> {
            AndroidLocalPhoneNameStorage(
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
