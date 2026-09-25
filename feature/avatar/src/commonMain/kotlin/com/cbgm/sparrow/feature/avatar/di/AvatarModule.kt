package com.cbgm.sparrow.feature.avatar.di

import com.cbgm.sparrow.feature.avatar.data.datasource.AvatarDataSource
import com.cbgm.sparrow.feature.avatar.data.datasource.LocalAvatarEditorDataSource
import com.cbgm.sparrow.feature.avatar.data.datasource.LocalAvatarImageDataSource
import com.cbgm.sparrow.feature.avatar.data.repository.AvatarEditorRepositoryImpl
import com.cbgm.sparrow.feature.avatar.data.repository.AvatarRepositoryImpl
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarRepository
import com.cbgm.sparrow.feature.avatar.domain.usecase.ClearAvatarEditorUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.ConsumeAvatarEditResultUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.CropAvatarEditorSourceUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.cbgm.sparrow.feature.avatar.domain.usecase.PrepareAvatarEditorSourceUseCase
import com.cbgm.sparrow.feature.avatar.presentation.AvatarViewModel
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val avatarModule =
    module {
        single {
            AvatarDataSource(
                localProfilePictureProvider = get(),
                remoteProfilePictureProvider = get(),
                groupAvatarProvider = get()
            )
        }

        single { LocalAvatarImageDataSource() }
        single { LocalAvatarEditorDataSource() }

        single<AvatarEditorRepository> {
            AvatarEditorRepositoryImpl(localDataSource = get())
        }

        factory { PrepareAvatarEditorSourceUseCase(repository = get()) }
        factory { CropAvatarEditorSourceUseCase(repository = get()) }
        factory { ConsumeAvatarEditResultUseCase(repository = get()) }
        factory { ClearAvatarEditorUseCase(repository = get()) }

        single<AvatarRepository> {
            AvatarRepositoryImpl(
                dataSource = get(),
                localAvatarImageDataSource = get(),
                applicationScope = get()
            )
        }

        factory { ObserveAvatarUseCase(repository = get()) }

        viewModel { parameters ->
            AvatarViewModel(
                target = parameters.get<AvatarTarget>(),
                observeAvatar = get()
            )
        }

        viewModel {
            AvatarEditorViewModel(
                prepareSource = get(),
                cropSource = get(),
                clearEditor = get()
            )
        }
    }
