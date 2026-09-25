package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarFileDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single {
            GroupAvatarFileDataSource(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }
    }
