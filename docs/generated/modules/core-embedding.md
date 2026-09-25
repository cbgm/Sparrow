# `:core:embedding`

Source directory: `core/embedding`

## Direct project dependencies

- `:core`
- `:data:datastore`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidLocalEmbeddingModelDownloader` | `class` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelDownloader.kt` |
| `AndroidLocalEmbeddingModelFiles` | `class` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelFiles.kt` |
| `AndroidLocalEmbeddingModelManager` | `class` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/AndroidLocalEmbeddingModelManager.kt` |
| `MediaPipeLocalTextEmbedder` | `class` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/MediaPipeLocalTextEmbedder.kt` |
| `LocalEmbeddingModelDownloadWorker` | `class` | `androidMain` | `core/embedding/src/androidMain/kotlin/com/cbgm/sparrow/core/embedding/work/LocalEmbeddingModelDownloadWorker.kt` |
| `LocalEmbeddingModel` | `object` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/model/LocalEmbeddingModel.kt` |
| `EmbeddingInputType` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalEmbeddingModelManager` | `interface` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalEmbeddingModelManager.kt` |
| `LocalTextEmbedder` | `interface` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/platform/LocalTextEmbedder.kt` |
| `LocalEmbeddingRepositoryImpl` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/repository/LocalEmbeddingRepositoryImpl.kt` |
| `LocalEmbeddingSettingsStorage` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/data/storage/LocalEmbeddingSettingsStorage.kt` |
| `LocalEmbeddingFeature` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingFeature.kt` |
| `LocalEmbeddingModelState` | `interface` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingModelState.kt` |
| `LocalEmbeddingState` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/model/LocalEmbeddingState.kt` |
| `LocalEmbeddingRepository` | `interface` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/repository/LocalEmbeddingRepository.kt` |
| `InitializeLocalEmbeddingUseCase` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/InitializeLocalEmbeddingUseCase.kt` |
| `ObserveLocalEmbeddingStateUseCase` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/ObserveLocalEmbeddingStateUseCase.kt` |
| `SetLocalEmbeddingFeatureEnabledUseCase` | `class` | `commonMain` | `core/embedding/src/commonMain/kotlin/com/cbgm/sparrow/core/embedding/domain/usecase/SetLocalEmbeddingFeatureEnabledUseCase.kt` |
