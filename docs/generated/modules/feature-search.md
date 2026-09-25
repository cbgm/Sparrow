# `:feature:search`

Source directory: `feature/search`

## Direct project dependencies

- `:core`
- `:core:embedding`
- `:core:ui`
- `:data:database`

## Production top-level Kotlin declarations

| Type | Kind | Source set | Source file |
|---|---|---|---|
| `MessageSearchIndexDataSource` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchIndexDataSource.kt` |
| `MessageSearchLocalDataSource` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/MessageSearchLocalDataSource.kt` |
| `SemanticSearchEmbeddingDataSource` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/datasource/SemanticSearchEmbeddingDataSource.kt` |
| `EmbeddingCodecMapper` | `object` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/mapper/EmbeddingCodecMapper.kt` |
| `SemanticSearchIndexConfig` | `object` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/model/SemanticSearchIndexConfig.kt` |
| `MessageSearchRepositoryImpl` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/MessageSearchRepositoryImpl.kt` |
| `SemanticSearchRepositoryImpl` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/data/repository/SemanticSearchRepositoryImpl.kt` |
| `MessageSearchConversationType` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchConversationType.kt` |
| `MessageSearchMatchType` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchMatchType.kt` |
| `MessageSearchResult` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/MessageSearchResult.kt` |
| `SemanticSearchState` | `interface` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/model/SemanticSearchState.kt` |
| `MessageSearchRepository` | `interface` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/MessageSearchRepository.kt` |
| `SemanticSearchRepository` | `interface` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/repository/SemanticSearchRepository.kt` |
| `InitializeSemanticSearchUseCase` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/InitializeSemanticSearchUseCase.kt` |
| `ObserveSemanticSearchStateUseCase` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/ObserveSemanticSearchStateUseCase.kt` |
| `SearchMessagesUseCase` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SearchMessagesUseCase.kt` |
| `SetSemanticSearchEnabledUseCase` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/domain/usecase/SetSemanticSearchEnabledUseCase.kt` |
| `MessageSearchViewModel` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/MessageSearchViewModel.kt` |
| `MessageSearchMode` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchMode.kt` |
| `MessageSearchResultUi` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchResultUi.kt` |
| `MessageSearchUiEvent` | `interface` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiEvent.kt` |
| `MessageSearchUiState` | `class` | `commonMain` | `feature/search/src/commonMain/kotlin/com/cbgm/sparrow/feature/search/presentation/overview/model/MessageSearchUiState.kt` |
