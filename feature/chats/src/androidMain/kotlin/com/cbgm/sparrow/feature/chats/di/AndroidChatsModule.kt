package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.attachment.storage.MessageAttachmentFileStorage
import com.cbgm.sparrow.feature.chats.data.group.storage.AndroidGroupKeyStorage
import com.cbgm.sparrow.feature.chats.data.group.storage.GroupAvatarFileStorage
import com.cbgm.sparrow.feature.chats.data.group.storage.GroupKeyStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single {
            MessageAttachmentFileStorage(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }

        single {
            GroupAvatarFileStorage(rootDirectory = androidContext().filesDir.absolutePath)
        }

        single<GroupKeyStorage> {
            AndroidGroupKeyStorage(dataStore = get())
        }
    }
