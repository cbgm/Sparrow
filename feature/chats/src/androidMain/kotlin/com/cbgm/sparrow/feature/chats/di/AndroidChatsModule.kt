package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.attachment.datasource.MessageAttachmentFileDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarFileDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupKeyDataSource
import com.cbgm.sparrow.feature.chats.device.AndroidGroupKeyDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single {
            MessageAttachmentFileDataSource(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }

        single {
            GroupAvatarFileDataSource(rootDirectory = androidContext().filesDir.absolutePath)
        }

        single<GroupKeyDataSource> {
            AndroidGroupKeyDataSource(dataStore = get())
        }
    }
