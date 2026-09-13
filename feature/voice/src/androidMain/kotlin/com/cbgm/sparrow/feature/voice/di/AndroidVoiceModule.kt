package com.cbgm.sparrow.feature.voice.di

import com.cbgm.sparrow.feature.voice.device.AndroidVoicePlayer
import com.cbgm.sparrow.feature.voice.device.AndroidVoiceRecorder
import com.cbgm.sparrow.feature.voice.device.AndroidVoiceTranscriptionRepository
import com.cbgm.sparrow.feature.voice.device.AndroidWhisperModelStore
import com.cbgm.sparrow.feature.voice.device.VoicePlayer
import com.cbgm.sparrow.feature.voice.device.VoiceRecorder
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformVoiceModule =
    module {
        single<VoiceRecorder> { AndroidVoiceRecorder(context = androidContext()) }
        single<VoicePlayer> { AndroidVoicePlayer() }
        single { AndroidWhisperModelStore(context = androidContext()) }
        single<VoiceTranscriptionRepository> { AndroidVoiceTranscriptionRepository(modelStore = get()) }
    }
