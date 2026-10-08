# Settings and diagnostics

Settings aggregates user-facing preferences and developer/operational diagnostics.

## App lock

`SettingsViewModel` observes `ObserveAppLockEnabledUseCase` and updates through `SetAppLockEnabledUseCase`. `SettingsScreen` authenticates through `AppLockAuthenticationLauncher` before applying a toggle change. Failed/unavailable/cancelled authentication does not silently change the preference.

See [App lock](app-lock.md).

## Local intelligence

Settings exposes semantic-search/message-safety controls and their model/download/index state. The shared embedding runtime lives in `:feature:embedding`.

## Attachments

Settings links to `AttachmentStorageRoute` and per-conversation `AttachmentManagementRoute`. Media/files are observed through the attachment repository and can be removed locally without changing the remote message protocol content.

## Network/developer diagnostics

Network/developer UI exposes Control Plane/node state, connection diagnostics and persisted developer error logs with timestamps/clear actions. The exact source inventory is available in the generated `:feature:settings` module page.
