# Auto reply

`:feature:autoreply` owns locally configured automatic replies and per-contact claiming so one active rule does not repeatedly reply to the same contact without the repository policy allowing it.

## Classes

- `AutoReply`
- `AutoReplyRepository` / `AutoReplyRepositoryImpl`
- `AutoReplyDataSource`
- mapping functions `AutoReplyEntity.toDomain()` / `AutoReply.toEntity()` in `AutoReplyMapper.kt`
- `CreateAutoReplyUseCase`
- `UpdateAutoReplyUseCase`
- `DeleteAutoReplyUseCase`
- `ActivateAutoReplyUseCase`
- `DeactivateAutoReplyUseCase`
- `ObserveAutoRepliesUseCase`
- `ObserveActiveAutoReplyUseCase`
- `ClaimAutoReplyForContactUseCase`
- `ReleaseAutoReplyRecipientUseCase`
- `AutoReplyViewModel`, `AutoReplySettingsRoute()`, `AutoReplySettingsScreen()`
- Room `AutoReplyEntity`, `AutoReplyRecipientEntity`, `AutoReplyDao`

`ClaimAutoReplyForContactUseCase` records the claim timestamp using `SystemClock` and returns the applicable reply if one is currently claimable for that contact. The actual sending still travels through normal chat/orchestration encryption and transport rules.
