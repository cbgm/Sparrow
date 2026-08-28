# Settings and diagnostics

`:feature:settings` owns the user-facing Settings screens plus developer/network diagnostics.

## Current settings areas

- profile settings;
- language selection;
- privacy/disclaimer pages;
- dependency/open-source licence information;
- Control Plane configuration;
- semantic-search enable/disable and preparation state;
- message-safety enable/disable and preparation/analysis state;
- attachment-storage navigation;
- developer diagnostics and persisted error log.

## Local AI feature controls

Semantic search and message safety have separate toggles but share the local embedding runtime in `:core:embedding`. Settings displays preparation/download progress and feature-specific ready/failure state. Disabling one feature does not imply that the shared model can be removed if the other still needs it.

## Attachment storage

The Settings overview links to `AttachmentStorageRoute`, owned by `:feature:attachments`. That screen summarizes saved attachments by conversation and allows the user to inspect/manage locally saved attachment copies.

## Developer menu

The Developer menu includes network diagnostics and the developer error log.

The error log:

- persists captured developer-facing errors;
- displays a visible timestamp for each entry;
- exposes a clear action with confirmation;
- is presented through `DeveloperErrorLogRoute` / `DeveloperErrorLogViewModel`.

The network diagnostics remain focused on Control Plane/node reachability, active-node state, connection counts and cooldown information.

## Important classes

- `SettingsViewModel`
- `ControlPlaneSettingsViewModel`
- `DeveloperMenuViewModel`
- `DeveloperErrorLogViewModel`
- `DeveloperErrorLogRepository`
- `DeveloperErrorLogRepositoryImpl`
