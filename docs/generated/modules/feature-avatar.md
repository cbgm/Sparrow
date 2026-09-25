# `:feature:avatar`

Source directory: `feature/avatar`

## Direct project dependencies

- `:core`
- `:core:protocol`
- `:core:ui`
- `:feature:media`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AvatarDataSource` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/AvatarDataSource.kt` |
| `CachedAvatarImage` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `LocalAvatarEditorDataSource` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarEditorDataSource.kt` |
| `LocalAvatarImageDataSource` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/datasource/LocalAvatarImageDataSource.kt` |
| `AvatarEditorRepositoryImpl` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarEditorRepositoryImpl.kt` |
| `AvatarRepositoryImpl` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/data/repository/AvatarRepositoryImpl.kt` |
| `ImagePickerLauncher` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.kt` |
| `Avatar` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/Avatar.kt` |
| `AvatarEditResult` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditResult.kt` |
| `AvatarEditorImage` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarEditorImage.kt` |
| `AvatarImage` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarImage.kt` |
| `AvatarTarget` | `interface` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/AvatarTarget.kt` |
| `ProfilePictureCropRegion` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `ProfilePictureSourceRect` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/model/ProfilePictureCropRegion.kt` |
| `AvatarEditorRepository` | `interface` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarEditorRepository.kt` |
| `AvatarRepository` | `interface` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/repository/AvatarRepository.kt` |
| `ClearAvatarEditorUseCase` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ClearAvatarEditorUseCase.kt` |
| `ConsumeAvatarEditResultUseCase` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ConsumeAvatarEditResultUseCase.kt` |
| `CropAvatarEditorSourceUseCase` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/CropAvatarEditorSourceUseCase.kt` |
| `ObserveAvatarUseCase` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/ObserveAvatarUseCase.kt` |
| `PrepareAvatarEditorSourceUseCase` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/domain/usecase/PrepareAvatarEditorSourceUseCase.kt` |
| `AvatarViewModel` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/AvatarViewModel.kt` |
| `AvatarEditorStrings` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditor.kt` |
| `AvatarEditorViewModel` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/AvatarEditorViewModel.kt` |
| `ProfilePictureCropGeometry` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/crop/ProfilePictureCropGeometry.kt` |
| `AvatarEditorUiState` | `class` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/editor/model/AvatarEditorUiState.kt` |
| `AvatarUiState` | `interface` | `commonMain` | `feature/avatar/src/commonMain/kotlin/com/cbgm/sparrow/feature/avatar/presentation/model/AvatarUiState.kt` |
| `ImagePickerDelegate` | `class` | `iosMain` | `feature/avatar/src/iosMain/kotlin/com/cbgm/sparrow/feature/avatar/device/ImagePicker.ios.kt` |
