# Message safety

`:feature:safety` implements optional on-device message-risk analysis. It is a user-facing warning aid, not a server moderation system and not a guarantee that a message is malicious or safe.

## Analysis

The current implementation combines structural analysis with the shared local text-embedding model. Detected reason categories include:

- suspicious link;
- lookalike domain;
- mixed-script domain;
- IP-address link;
- URL shortener;
- urgent-action request;
- credential request;
- payment request;
- private-key request.

The embedding-based classifier runs locally through the shared `core:embedding` runtime when available.

## UI behavior

When a message has safety reasons, chat presentation can show a warning. The details route explains the detected reasons and provides a block-user action where the conversation context supports it.

Settings exposes the feature toggle plus preparation/downloading/analyzing/ready/failure state.

## Important classes

- `AnalyzeMessageSafetyUseCase`
- `InitializeMessageSafetyUseCase`
- `ObserveMessageSafetyAssessmentsUseCase`
- `ProcessMessageSafetyBatchUseCase`
- `MessageSafetyAnalysisRepository` / `MessageSafetyAnalysisRepositoryImpl`
- `MessageSafetyRepository` / `MessageSafetyRepositoryImpl`
- `MessageSafetyStructuralAnalyzer`
- `MessageSafetyClassifier`
- `MessageSafetyDetailsViewModel`
