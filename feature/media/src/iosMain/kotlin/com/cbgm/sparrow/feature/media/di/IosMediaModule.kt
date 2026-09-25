package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.device.IosFileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.IosMediaSelectionFileDataSource
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformMediaModule =
    module {
        single<MediaSelectionFileDataSource> { IosMediaSelectionFileDataSource() }
        singleOf(::IosFileBrowserDataSource) {
            bind<FileBrowserDataSource>()
        }
    }
