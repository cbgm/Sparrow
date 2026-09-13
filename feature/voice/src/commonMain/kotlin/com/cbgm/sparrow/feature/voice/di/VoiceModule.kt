package com.cbgm.sparrow.feature.voice.di

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadAttachmentBytesUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.ObserveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.attachments.domain.usecase.SaveMessageAttachmentTranscriptUseCase
import com.cbgm.sparrow.feature.voice.data.datasource.VoiceTranscriptionSettingsDataSource
import com.cbgm.sparrow.feature.voice.data.repository.VoiceRepositoryImpl
import com.cbgm.sparrow.feature.voice.data.repository.VoiceTranscriptionSettingsRepositoryImpl
import com.cbgm.sparrow.feature.voice.device.VoicePlayer
import com.cbgm.sparrow.feature.voice.device.VoiceRecorder
import com.cbgm.sparrow.feature.voice.domain.model.VoiceMessageTarget
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceRepository
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionSettingsRepository
import com.cbgm.sparrow.feature.voice.domain.usecase.CancelVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.FinishVoiceMessageScrubUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.GetRecordedVoiceAttachmentUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceComposerUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoicePlaybackUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceRecordingActiveUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceTranscriptionEnabledUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.PrepareVoiceTranscriptionUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ResetVoiceComposerUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.SetVoiceTranscriptionEnabledUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StartVoiceMessageScrubUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StartVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.StopVoiceRecordingUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ToggleVoiceMessagePlaybackUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ToggleVoicePreviewUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.TranscribeVoiceAudioUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.TranscribeVoiceMessageUseCase
import com.cbgm.sparrow.feature.voice.presentation.composer.VoiceComposerViewModel
import com.cbgm.sparrow.feature.voice.presentation.message.VoiceMessageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val voiceModule =
    module {
        single { VoiceTranscriptionSettingsDataSource(dataStore = get()) }
        single<VoiceTranscriptionSettingsRepository> {
            VoiceTranscriptionSettingsRepositoryImpl(dataSource = get())
        }

        single<VoiceRepository> {
            VoiceRepositoryImpl(
                applicationScope = get<ApplicationCoroutineScope>(),
                recorder = get<VoiceRecorder>(),
                player = get<VoicePlayer>()
            )
        }

        factory { ObserveVoiceComposerUseCase(repository = get()) }
        factory { ObserveVoiceRecordingActiveUseCase(repository = get()) }
        factory { StartVoiceRecordingUseCase(repository = get()) }
        factory { StopVoiceRecordingUseCase(repository = get()) }
        factory { ToggleVoicePreviewUseCase(repository = get()) }
        factory { CancelVoiceRecordingUseCase(repository = get()) }
        factory { ResetVoiceComposerUseCase(repository = get()) }
        factory { GetRecordedVoiceAttachmentUseCase(repository = get()) }
        factory { ObserveVoicePlaybackUseCase(repository = get()) }
        factory {
            ToggleVoiceMessagePlaybackUseCase(
                loadAttachmentBytes = get<LoadAttachmentBytesUseCase>(),
                repository = get<VoiceRepository>()
            )
        }
        factory { StartVoiceMessageScrubUseCase(repository = get()) }
        factory {
            FinishVoiceMessageScrubUseCase(
                loadAttachmentBytes = get<LoadAttachmentBytesUseCase>(),
                repository = get<VoiceRepository>()
            )
        }
        factory { ObserveVoiceTranscriptionEnabledUseCase(repository = get()) }
        factory { SetVoiceTranscriptionEnabledUseCase(repository = get()) }
        factory { PrepareVoiceTranscriptionUseCase(repository = get<VoiceTranscriptionRepository>()) }
        factory { TranscribeVoiceAudioUseCase(repository = get<VoiceTranscriptionRepository>()) }
        factory {
            TranscribeVoiceMessageUseCase(
                loadAttachmentBytes = get<LoadAttachmentBytesUseCase>(),
                prepareVoiceTranscription = get<PrepareVoiceTranscriptionUseCase>(),
                transcribeVoiceAudio = get<TranscribeVoiceAudioUseCase>(),
                saveTranscript = get<SaveMessageAttachmentTranscriptUseCase>()
            )
        }

        viewModel {
            VoiceComposerViewModel(
                observeVoiceComposer = get(),
                startVoiceRecording = get(),
                stopVoiceRecording = get(),
                toggleVoicePreview = get(),
                cancelVoiceRecording = get()
            )
        }

        viewModel { parameters ->
            VoiceMessageViewModel(
                target = parameters.get<VoiceMessageTarget>(),
                observeVoicePlayback = get(),
                observeTranscriptionEnabled = get(),
                observePersistedTranscript = get<ObserveMessageAttachmentTranscriptUseCase>(),
                toggleVoicePlayback = get(),
                startVoiceScrub = get(),
                finishVoiceScrub = get(),
                transcribeVoiceMessage = get()
            )
        }
    }
