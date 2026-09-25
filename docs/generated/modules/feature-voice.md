# `:feature:voice`

Source directory: `feature/voice`

## Direct project dependencies

- `:core`
- `:core:protocol`
- `:core:ui`
- `:data:datastore`
- `:feature:attachments`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidVoicePlayer` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `AndroidVoiceRecorder` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceRecorder.kt` |
| `AndroidVoiceTranscriptionRepository` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoiceTranscriptionRepository.kt` |
| `AndroidWhisperModelStore` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidWhisperModelStore.kt` |
| `ByteArrayMediaDataSource` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/AndroidVoicePlayer.kt` |
| `WhisperNative` | `class` | `androidMain` | `feature/voice/src/androidMain/kotlin/com/cbgm/sparrow/feature/voice/device/WhisperNative.kt` |
| `VoiceTranscriptionSettingsDataSource` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/datasource/VoiceTranscriptionSettingsDataSource.kt` |
| `VoiceRepositoryImpl` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceRepositoryImpl.kt` |
| `VoiceTranscriptionSettingsRepositoryImpl` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/data/repository/VoiceTranscriptionSettingsRepositoryImpl.kt` |
| `PcmWaveAudio` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/PcmWaveAudio.kt` |
| `VoicePlayer` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoicePlayer.kt` |
| `VoiceRecorder` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/device/VoiceRecorder.kt` |
| `VoiceComposerPhase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceComposerState` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceMessageTarget` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceMessageTarget.kt` |
| `VoicePlaybackState` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceRecording` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceRecording.kt` |
| `VoiceTranscript` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceTranscriptCue` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceTranscript.kt` |
| `VoiceTranscriptionState` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/model/VoiceState.kt` |
| `VoiceRepository` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceRepository.kt` |
| `VoiceTranscriptionRepository` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionRepository.kt` |
| `VoiceTranscriptionSettingsRepository` | `interface` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/repository/VoiceTranscriptionSettingsRepository.kt` |
| `CancelVoiceRecordingUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/CancelVoiceRecordingUseCase.kt` |
| `FinishVoiceMessageScrubUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/FinishVoiceMessageScrubUseCase.kt` |
| `GetRecordedVoiceAttachmentUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/GetRecordedVoiceAttachmentUseCase.kt` |
| `ObserveVoiceComposerUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceComposerUseCase.kt` |
| `ObserveVoicePlaybackUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoicePlaybackUseCase.kt` |
| `ObserveVoiceRecordingActiveUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceRecordingActiveUseCase.kt` |
| `ObserveVoiceTranscriptionEnabledUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ObserveVoiceTranscriptionEnabledUseCase.kt` |
| `PrepareVoiceTranscriptionUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/PrepareVoiceTranscriptionUseCase.kt` |
| `ResetVoiceComposerUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ResetVoiceComposerUseCase.kt` |
| `SetVoiceTranscriptionEnabledUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/SetVoiceTranscriptionEnabledUseCase.kt` |
| `StartVoiceMessageScrubUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceMessageScrubUseCase.kt` |
| `StartVoiceRecordingUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StartVoiceRecordingUseCase.kt` |
| `StopVoiceRecordingUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/StopVoiceRecordingUseCase.kt` |
| `ToggleVoiceMessagePlaybackUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoiceMessagePlaybackUseCase.kt` |
| `ToggleVoicePreviewUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/ToggleVoicePreviewUseCase.kt` |
| `TranscribeVoiceAudioUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceAudioUseCase.kt` |
| `TranscribeVoiceMessageUseCase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhase` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceTranscriptionPhaseException` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/domain/usecase/TranscribeVoiceMessageUseCase.kt` |
| `VoiceAction` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposer.kt` |
| `VoiceComposerViewModel` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/VoiceComposerViewModel.kt` |
| `VoiceComposerUiState` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/composer/model/VoiceComposerUiState.kt` |
| `VoiceMessageViewModel` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageViewModel.kt` |
| `VoiceScrubState` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/VoiceMessageContent.kt` |
| `VoiceMessageUiState` | `class` | `commonMain` | `feature/voice/src/commonMain/kotlin/com/cbgm/sparrow/feature/voice/presentation/message/model/VoiceMessageUiState.kt` |
| `IosVoicePlayer` | `class` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoicePlayer.kt` |
| `IosVoiceRecorder` | `class` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceRecorder.kt` |
| `IosVoiceTranscriptionRepository` | `class` | `iosMain` | `feature/voice/src/iosMain/kotlin/com/cbgm/sparrow/feature/voice/device/IosVoiceTranscriptionRepository.kt` |
