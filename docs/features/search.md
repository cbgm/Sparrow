# Message search

`:feature:search` provides local message search for Direct and Group conversations. Search works without semantic search; the optional semantic mode adds on-device embedding similarity results.

## Search modes

The UI exposes these effective states:

- exact-only search when semantic search is disabled/unavailable;
- preparing/downloading/index-building while the local model is being prepared;
- hybrid search when semantic search is ready.

`SearchMessagesUseCase` always queries exact local search first. If there is room in the result limit, it adds semantic results that are not already represented by the exact result set. If semantic search fails, exact results are still returned.

## Local embedding model

`core:embedding` owns the shared local embedding runtime. On Android it uses MediaPipe Text Embedder. The model is downloaded/prepared on device, stored locally and integrity-checked before being used.

The same local embedding runtime can be required by semantic search and message safety; enabling either feature can therefore require the shared model. No message text is sent to a cloud AI service for semantic search.

## Indexing and navigation

Semantic search builds a local message embedding index. Search results preserve Direct/Group conversation identity and message IDs, allowing result selection to navigate to the matching conversation/message.

## Important classes

- `SearchMessagesUseCase`
- `InitializeSemanticSearchUseCase`
- `ObserveSemanticSearchStateUseCase`
- `SetSemanticSearchEnabledUseCase`
- `MessageSearchRepository` / `MessageSearchRepositoryImpl`
- `SemanticSearchRepository` / `SemanticSearchRepositoryImpl`
- `MessageSearchIndexDataSource`
- `SemanticSearchEmbeddingDataSource`
- `MessageSearchViewModel`
