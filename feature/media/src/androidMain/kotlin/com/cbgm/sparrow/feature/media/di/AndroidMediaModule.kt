package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.device.AndroidFileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.AndroidMediaSelectionFileDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformMediaModule =
    module {
        single<MediaSelectionFileDataSource> { AndroidMediaSelectionFileDataSource(androidContext()) }
        single<FileBrowserDataSource> {
            AndroidFileBrowserDataSource(context = androidContext())
        }
    }
