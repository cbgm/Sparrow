package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.repository.FileBrowserRepositoryImpl
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository
import com.cbgm.sparrow.feature.media.domain.usecase.BrowseFileDirectoryUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.CheckFileBrowserAccessUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.GetFileBrowserRootUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.ReadFileBrowserEntryUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.SetFileBrowserRootUseCase
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerSessionController
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mediaModule =
    module {
        singleOf(::FileBrowserRepositoryImpl) {
            bind<FileBrowserRepository>()
        }
        factory { CheckFileBrowserAccessUseCase(repository = get()) }
        factory { SetFileBrowserRootUseCase(repository = get()) }
        factory { GetFileBrowserRootUseCase(repository = get()) }
        factory { BrowseFileDirectoryUseCase(repository = get()) }
        factory { ReadFileBrowserEntryUseCase(repository = get()) }
        singleOf(::FilePickerSessionController)
        viewModel {
            FilePickerViewModel(
                savedStateHandle = get(),
                sessions = get(),
                checkAccess = get(),
                setRoot = get(),
                getRoot = get(),
                browseDirectory = get(),
                readFile = get()
            )
        }
    }
