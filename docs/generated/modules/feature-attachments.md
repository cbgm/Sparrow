# `:feature:attachments`

Source directory: `feature/attachments`

## Direct project dependencies

- `:core`
- `:core:crypto`
- `:core:protocol`
- `:core:ui`
- `:data:database`
- `:feature:media`
- `:feature:transport`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `AndroidLocationOpener` | `class` | `androidMain` | `feature/attachments/src/androidMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.android.kt` |
| `AttachmentContentDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/AttachmentContentDataSource.kt` |
| `BlobTransferDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/BlobTransferDataSource.kt` |
| `LocalAttachmentContentDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentContentDataSource.kt` |
| `LocalAttachmentDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/LocalAttachmentDataSource.kt` |
| `MessageAttachmentDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentDataSource.kt` |
| `MessageAttachmentFileDataSource` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/datasource/MessageAttachmentFileDataSource.kt` |
| `AttachmentContentPayloadDto` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentContentPayloadDto.kt` |
| `AttachmentMessageContextDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentMessageContextDto.kt` |
| `AttachmentStorageSummaryDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentStorageSummaryDto.kt` |
| `AttachmentTargetDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/AttachmentTargetDto.kt` |
| `OutgoingMessageAttachmentDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/OutgoingMessageAttachmentDto.kt` |
| `PreparedMessageAttachmentDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/PreparedMessageAttachmentDto.kt` |
| `UploadedBlobDto` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/model/UploadedBlobDto.kt` |
| `BlobTransferRepositoryImpl` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/BlobTransferRepositoryImpl.kt` |
| `MessageAttachmentOperationsRepositoryImpl` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentOperationsRepositoryImpl.kt` |
| `MessageAttachmentRepositoryImpl` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/data/repository/MessageAttachmentRepositoryImpl.kt` |
| `CurrentLocationLauncher` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.kt` |
| `LocationOpener` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.kt` |
| `AttachmentContent` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentContent.kt` |
| `AttachmentMessageContext` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentMessageContext.kt` |
| `AttachmentSource` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTarget.kt` |
| `AttachmentStorageSummary` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentStorageSummary.kt` |
| `AttachmentTarget` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTarget.kt` |
| `AttachmentTranscript` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `AttachmentTranscriptCue` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/AttachmentTranscript.kt` |
| `CurrentLocation` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/CurrentLocation.kt` |
| `LocalAttachment` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/LocalAttachment.kt` |
| `MessageAttachment` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/MessageAttachment.kt` |
| `MessageAttachmentPolicy` | `object` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/MessageAttachmentPolicy.kt` |
| `OutgoingMessageAttachment` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/OutgoingMessageAttachment.kt` |
| `PreparedMessageAttachment` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/PreparedMessageAttachment.kt` |
| `SharedContact` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/SharedContact.kt` |
| `UploadedBlob` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/model/UploadedBlob.kt` |
| `BlobTransferRepository` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/BlobTransferRepository.kt` |
| `MessageAttachmentOperationsRepository` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentOperationsRepository.kt` |
| `MessageAttachmentRepository` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/repository/MessageAttachmentRepository.kt` |
| `DeleteBlobUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteBlobUseCase.kt` |
| `DeleteConversationLocalAttachmentsUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteConversationLocalAttachmentsUseCase.kt` |
| `DeleteLocalAttachmentsUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DeleteLocalAttachmentsUseCase.kt` |
| `DownloadBlobUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/DownloadBlobUseCase.kt` |
| `LoadAttachmentBytesUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentBytesUseCase.kt` |
| `LoadAttachmentContentUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadAttachmentContentUseCase.kt` |
| `LoadMessageAttachmentUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/LoadMessageAttachmentUseCase.kt` |
| `ObserveAttachmentStorageSummariesUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveAttachmentStorageSummariesUseCase.kt` |
| `ObserveLocalAttachmentsUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveLocalAttachmentsUseCase.kt` |
| `ObserveMessageAttachmentTranscriptUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/ObserveMessageAttachmentTranscriptUseCase.kt` |
| `SaveMessageAttachmentTranscriptUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/SaveMessageAttachmentTranscriptUseCase.kt` |
| `UploadBlobUseCase` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/domain/usecase/UploadBlobUseCase.kt` |
| `AttachmentViewModel` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/AttachmentViewModel.kt` |
| `AttachmentManagementLocalState` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementViewModel` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentSnapshot` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/AttachmentManagementViewModel.kt` |
| `AttachmentManagementTab` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementTab.kt` |
| `AttachmentManagementUiEvent` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiEvent.kt` |
| `AttachmentManagementUiState` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/management/model/AttachmentManagementUiState.kt` |
| `AttachmentUiState` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/AttachmentUiState.kt` |
| `MessageAttachmentUi` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/model/MessageAttachmentUi.kt` |
| `AttachmentStorageViewModel` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/AttachmentStorageViewModel.kt` |
| `AttachmentStorageUiEvent` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiEvent.kt` |
| `AttachmentStorageUiState` | `interface` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/presentation/storage/model/AttachmentStorageUiState.kt` |
| `MessageAttachmentCacheCoordinator` | `class` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/runtime/MessageAttachmentCacheCoordinator.kt` |
| `ContactAttachmentPayload` | `object` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/ContactAttachmentPayload.kt` |
| `LocationAttachmentPayload` | `object` | `commonMain` | `feature/attachments/src/commonMain/kotlin/com/cbgm/sparrow/feature/attachments/util/LocationAttachmentPayload.kt` |
| `CurrentLocationDelegate` | `class` | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/CurrentLocationLauncher.ios.kt` |
| `IosLocationOpener` | `class` | `iosMain` | `feature/attachments/src/iosMain/kotlin/com/cbgm/sparrow/feature/attachments/device/LocationOpener.ios.kt` |
