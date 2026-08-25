package com.cbgm.sparrow.feature.attachments.di

import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentFileDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidAttachmentsModule =
    module {
        single {
            MessageAttachmentFileDataSource(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }
    }
