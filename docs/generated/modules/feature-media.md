# `:feature:media`

Source directory: `feature/media`

## Direct project dependencies

- `:core`
- `:core:ui`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidFileBrowserDataSource` | `class` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidFileBrowserDataSource.kt` |
| `AndroidFileOpener` | `class` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.android.kt` |
| `AndroidMediaExporter` | `class` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.android.kt` |
| `AndroidMediaSelectionFileDataSource` | `class` | `androidMain` | `feature/media/src/androidMain/kotlin/com/cbgm/sparrow/feature/media/device/AndroidMediaSelectionFileDataSource.kt` |
| `FileBrowserDataSource` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/FileBrowserDataSource.kt` |
| `MediaSelectionFileDataSource` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/datasource/MediaSelectionFileDataSource.kt` |
| `FileBrowserContentDto` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserContentDto.kt` |
| `FileBrowserDirectoryDto` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserDirectoryDto.kt` |
| `FileBrowserEntryDto` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/FileBrowserEntryDto.kt` |
| `StoredMediaSelectionPathsDto` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/model/StoredMediaSelectionPathsDto.kt` |
| `FileBrowserRepositoryImpl` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/FileBrowserRepositoryImpl.kt` |
| `MediaSelectionFileRepositoryImpl` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/data/repository/MediaSelectionFileRepositoryImpl.kt` |
| `CameraCaptureLauncher` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.kt` |
| `FileAccessLauncher` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.kt` |
| `FileOpener` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.kt` |
| `GalleryPickerLauncher` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `GalleryPickerStrings` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.kt` |
| `MediaExporter` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/device/MediaExport.kt` |
| `CameraCaptureConfig` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraCaptureType` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CameraLens` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `CapturedMedia` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/CameraCapture.kt` |
| `FileBrowserContent` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserContent.kt` |
| `FileBrowserDirectory` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserDirectory.kt` |
| `FileBrowserEntry` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/FileBrowserEntry.kt` |
| `GalleryMedia` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `GalleryPickerConfig` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/GalleryMedia.kt` |
| `MediaContentType` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaContentType.kt` |
| `MediaExportItem` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/MediaExportItem.kt` |
| `StoredMediaSelectionPaths` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/model/StoredMediaSelectionPaths.kt` |
| `FileBrowserRepository` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/FileBrowserRepository.kt` |
| `MediaSelectionFileRepository` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/repository/MediaSelectionFileRepository.kt` |
| `BrowseFileDirectoryUseCase` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/BrowseFileDirectoryUseCase.kt` |
| `CheckFileBrowserAccessUseCase` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/CheckFileBrowserAccessUseCase.kt` |
| `GetFileBrowserRootUseCase` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/GetFileBrowserRootUseCase.kt` |
| `ReadFileBrowserEntryUseCase` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/ReadFileBrowserEntryUseCase.kt` |
| `SetFileBrowserRootUseCase` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/domain/usecase/SetFileBrowserRootUseCase.kt` |
| `DirectoryState` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `FilePickerLauncher` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerLauncher.kt` |
| `FilePickerSession` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionController` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerSessionSnapshot` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerSessionController.kt` |
| `FilePickerViewModel` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/FilePickerViewModel.kt` |
| `FileBrowserEntryKind` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FileBrowserEntryUi` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerBreadcrumbUi` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerSessionResult` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerSessionResult.kt` |
| `FilePickerSortMode` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `FilePickerUiEvent` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiEvent.kt` |
| `FilePickerUiState` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/filepicker/model/FilePickerUiState.kt` |
| `MediaItem` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItem.kt` |
| `MediaSelection` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaSelectionResult` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelectionResult.kt` |
| `MediaSelectionSource` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaSelectionType` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaSelection.kt` |
| `MediaType` | `class` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/model/MediaItem.kt` |
| `MediaSelectionLauncher` | `interface` | `commonMain` | `feature/media/src/commonMain/kotlin/com/cbgm/sparrow/feature/media/presentation/selection/MediaSelectionLauncher.kt` |
| `CameraPickerDelegate` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/CameraCapture.ios.kt` |
| `DocumentInteractionDelegate` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `FileAccessDelegate` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileAccessLauncher.ios.kt` |
| `GalleryPickerDelegate` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `GallerySelectionCollector` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/GalleryPicker.ios.kt` |
| `IosFileBrowserDataSource` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosFileBrowserDataSource.kt` |
| `IosFileOpener` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/FileOpener.ios.kt` |
| `IosMediaSelectionFileDataSource` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/IosMediaSelectionFileDataSource.kt` |
| `VideoView` | `class` | `iosMain` | `feature/media/src/iosMain/kotlin/com/cbgm/sparrow/feature/media/device/PlatformVideoMedia.ios.kt` |
