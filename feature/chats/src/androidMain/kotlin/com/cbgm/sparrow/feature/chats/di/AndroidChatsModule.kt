package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.group.storage.AndroidGroupKeyStorage
import com.cbgm.sparrow.feature.chats.data.group.storage.GroupKeyStorage
import org.koin.dsl.module

val androidChatsModule =
    module {
        single<GroupKeyStorage> {
            AndroidGroupKeyStorage(dataStore = get())
        }
    }
