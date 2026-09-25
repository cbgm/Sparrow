# Voice messages and local transcription

Voice is implemented as attachment-backed message content in `:feature:voice`; it is not a separate transport/message protocol.

## Main classes

Recording/playback abstraction:

- `VoiceRepository` / `VoiceRepositoryImpl`
- `VoiceRecorder`, `VoicePlayer`
- `AndroidVoiceRecorder`, `AndroidVoicePlayer`
- iOS adapters `IosVoiceRecorder`, `IosVoicePlayer`
- `PcmWaveAudio`

Composer:

- `VoiceComposer`
- `VoiceComposerViewModel`
- `VoiceComposerUiState`
- `StartVoiceRecordingUseCase`
- `StopVoiceRecordingUseCase`
- `CancelVoiceRecordingUseCase`
- `ToggleVoicePreviewUseCase`
- `ObserveVoiceComposerUseCase`

Message playback:

- `VoiceMessageContent`
- `VoiceMessageViewModel`
- `VoiceMessageTarget`
- `ObserveVoicePlaybackUseCase`
- `ToggleVoiceMessagePlaybackUseCase`
- `StartVoiceMessageScrubUseCase`
- `FinishVoiceMessageScrubUseCase`

Transcription:

- `TranscribeVoiceMessageUseCase`
- `VoiceTranscriptionState`
- `VoiceTranscript`, `VoiceTranscriptCue`
- `AndroidVoiceTranscriptionRepository`
- `AndroidWhisperModelStore`
- `WhisperNative`
- `VoiceTranscriptionSettingsRepositoryImpl`
- `ObserveVoiceTranscriptionEnabledUseCase`
- attachment-owned `ObserveMessageAttachmentTranscriptUseCase`

## Recording flow

```mermaid
sequenceDiagram
    participant UI as VoiceComposer
    participant VM as VoiceComposerViewModel
    participant UC as voice use cases
    participant R as VoiceRepositoryImpl
    participant REC as AndroidVoiceRecorder
    participant CHAT as Chats composer

    UI->>VM: startRecording()
    VM->>UC: StartVoiceRecordingUseCase
    UC->>R: startRecording()
    R->>REC: record PCM/audio
    UI->>VM: stopRecording()
    VM->>UC: StopVoiceRecordingUseCase
    UC->>R: stopRecording()
    R-->>CHAT: VoiceRecording attachment source
```

The resulting recording is passed through the existing encrypted attachment/message path.

## Playback isolation

`VoiceRepository.observePlaybackState(attachmentId)` is keyed per attachment. `VoiceMessageViewModel` owns the UI state for one `VoiceMessageTarget`; this prevents unrelated voice bubbles from sharing the wrong duration/playback state.

## Transcription flow

```mermaid
sequenceDiagram
    participant VM as VoiceMessageViewModel
    participant UC as TranscribeVoiceMessageUseCase
    participant MODEL as AndroidWhisperModelStore
    participant WH as AndroidVoiceTranscriptionRepository / WhisperNative
    participant ATT as attachment transcript persistence

    VM->>UC: transcribe(target)
    UC->>MODEL: ensure model available
    MODEL-->>UC: model/progress
    UC->>WH: transcribe audio
    WH-->>UC: text + timestamp cues
    UC->>ATT: persist transcript
    ATT-->>VM: ObserveMessageAttachmentTranscriptUseCase
```

Transcription is local. Persisted transcript state is combined with live transcription progress and playback state in `VoiceMessageViewModel`.
