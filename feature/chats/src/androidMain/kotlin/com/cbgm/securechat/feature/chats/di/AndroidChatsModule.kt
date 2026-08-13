package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.feature.chats.data.security.AndroidGroupKeyStorage
import com.cbgm.securechat.feature.chats.domain.repository.GroupKeyRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single<GroupKeyRepository> {
            AndroidGroupKeyStorage(context = androidContext())
        }
    }
