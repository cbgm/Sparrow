package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.group.repository.GroupKeyRepositoryImpl
import com.cbgm.sparrow.feature.chats.data.group.storage.AndroidGroupKeyStorage
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupKeyRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single {
            AndroidGroupKeyStorage(context = androidContext())
        }
        single<GroupKeyRepository> {
            GroupKeyRepositoryImpl(storage = get())
        }
    }
