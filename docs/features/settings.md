# Settings and diagnostics

Settings aggregates user-facing preferences and developer/operational diagnostics.

## Sections

| Section | Entries |
|---|---|
| General | Language, theme (like the phone, light or dark) |
| Messaging | Auto reply |
| Network | Control Planes, preferred node, onion routing |
| Security | App lock, automatic secure setup, block unknown invitations, blocked contacts |
| Local intelligence | Semantic message search, scam and phishing protection, voice transcription |
| Storage | Media and files |
| Privacy & data | Privacy policy, data disclaimer |
| About | Open source licenses (the libraries the app uses), version, developer menu |

## Network

**Onion routing** sets the hops per circuit (1–5), the number of circuits (1–3), decoy envelopes (0–5) and
"Send directly without a route"; the screen shows the current route, its round trip and warnings when fewer
operators than chosen run relaying nodes. **Preferred node** picks automatic or one favourite gateway. Both are
described in [Transport](transport.md#onion-circuits).

## Privacy & data

`DisclaimerRoute` shows the privacy policy or the data disclaimer (`DisclaimerType`) as Markdown
(`MarkdownDisclaimerScreen`). The texts are the string resources `feature_settings_privacy_policy_content` and
`feature_settings_data_disclaimer_content` in `strings_settings.xml` (English and German). They describe what stays
on the phone, what Community Nodes and Control Planes keep and for how long, phone verification through Firebase,
the unencrypted first invite, metadata, onion routing and decoys. When behaviour they describe changes, update both
languages together.

## App lock

`SettingsViewModel` observes `ObserveAppLockEnabledUseCase` and updates through `SetAppLockEnabledUseCase`. `SettingsScreen` authenticates through `AppLockAuthenticationLauncher` before applying a toggle change. Failed/unavailable/cancelled authentication does not silently change the preference.

See [App lock](app-lock.md).

## Local intelligence

Settings exposes semantic-search/message-safety controls and their model/download/index state. The shared embedding runtime lives in `:feature:embedding`.

## Attachments

Settings links to `AttachmentStorageRoute` and per-conversation `AttachmentManagementRoute`. Media/files are observed through the attachment repository and can be removed locally without changing the remote message protocol content.

## Network/developer diagnostics

Network/developer UI exposes Control Plane/node state, connection diagnostics and persisted developer error logs with timestamps/clear actions. The exact source inventory is available in the generated `:feature:settings` module page.
