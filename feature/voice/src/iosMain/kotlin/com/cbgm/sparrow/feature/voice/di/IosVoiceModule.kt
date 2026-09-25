package com.cbgm.sparrow.feature.voice.di

import com.cbgm.sparrow.feature.voice.device.IosVoicePlayer
import com.cbgm.sparrow.feature.voice.device.IosVoiceRecorder
import com.cbgm.sparrow.feature.voice.device.IosVoiceTranscriptionRepository
import com.cbgm.sparrow.feature.voice.device.VoicePlayer
import com.cbgm.sparrow.feature.voice.device.VoiceRecorder
import com.cbgm.sparrow.feature.voice.domain.repository.VoiceTranscriptionRepository
import org.koin.dsl.module

actual val platformVoiceModule =
    module {
        single<VoiceRecorder> { IosVoiceRecorder() }
        single<VoicePlayer> { IosVoicePlayer() }
        single<VoiceTranscriptionRepository> { IosVoiceTranscriptionRepository() }
    }
