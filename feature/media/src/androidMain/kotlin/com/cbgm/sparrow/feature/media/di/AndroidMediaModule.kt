package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.AndroidFileBrowserDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformMediaModule =
    module {
        single<FileBrowserDataSource> {
            AndroidFileBrowserDataSource(context = androidContext())
        }
    }
