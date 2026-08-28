# Attachments

`:feature:attachments` owns the attachment source/transfer/cache/storage behavior. `:feature:media` owns platform media/file selection, rendering/opening and export helpers. `:feature:chats` maps attachment source data into its typed message-part representation for Direct and Group conversation UI.

## Supported attachment types

| Type | Current behavior |
|---|---|
| Image | Gallery/camera selection, thumbnail preview, viewer, received local copy |
| Video | Gallery selection, thumbnail + play indicator, tap-to-play viewer, received local copy |
| File | File-browser selection, filename/size bubble, download/open on tap, received local copy |
| Location | Current location is encoded as an attachment blob and sent immediately |
| Contact | Existing Contacts UI selects one contact; name/number are encoded as an attachment blob and sent immediately |

All attachment types currently use the encrypted blob attachment pipeline. Location/contact are intentionally still attachments so their payload can evolve later without inventing a separate message-content transport.

## Limits

`MessageAttachmentPolicy` and protocol constraints enforce:

- at most 8 attachments per message;
- images: at most 4 MiB each;
- videos: at most 64 MiB each;
- files: at most 96 MiB each;
- at most 96 MiB total selected attachment bytes per message;
- image dimensions are normalized/limited by the media preparation path.

## Sending

The attachment bar exposes gallery, camera, file, contact and current-location actions. Image/video/file selections can coexist with normal text. Location/contact are single-shot attachment actions and are sent without requiring extra text. Current-location permission is requested when the location action is used rather than as a permanent onboarding requirement.

Before the message packet is queued, attachment payloads are prepared and uploaded through the blob-transfer layer. The message packet contains typed attachment metadata plus an encrypted blob reference rather than embedding large raw bytes in the normal chat packet.

## Chat-domain representation

Chats do not expose the attachment module's source model as their conversation content model. The current layering is:

```text
feature/attachments source/transfer model
        |
        v
feature/chats data MessagePartDto
        |
        v
feature/chats domain MessagePart
        |
        v
feature/chats presentation MessagePartUi
```

The chat part hierarchy is typed: text, image/video, file, location and contact have distinct variants. This keeps attachment transport/storage ownership in `:feature:attachments` while keeping chat data/domain/presentation independent from one generic nullable-everything content model.

## Bubble and viewer behavior

- image/video bubbles show at most three media previews; additional media is represented by a `+N` tile;
- media viewer supports swiping between message media;
- videos never autoplay;
- file bubbles show filename and readable byte size and open the local/downloaded file on tap;
- location bubbles show coordinates after the payload is loaded and open through the platform location opener;
- contact bubbles show the available display name plus phone number;
- tapping a loaded contact asks for confirmation before adding it to device contacts.

## Storage behavior

Incoming binary image/video/file attachments get a saved conversation copy. Location/contact blobs are excluded from that media/files copy because they are structured attachment payloads rather than user-managed files.

Settings exposes attachment-storage summaries and management screens. The attachment module distinguishes a private message-attachment cache from the user-visible saved conversation copies. Sender-side cache data is not treated as a received saved-copy entry.

## Important classes

- `BlobTransferRepository` / `BlobTransferRepositoryImpl`
- `MessageAttachmentRepository` / `MessageAttachmentRepositoryImpl`
- `MessageAttachmentDataSource`
- `MessageAttachmentFileDataSource`
- `LocalAttachmentDataSource`
- `MessageAttachmentCacheCoordinator`
- `OutgoingMessageAttachment`
- `MessageAttachmentPolicy`
- `LocationAttachmentPayload`
- `ContactAttachmentPayload`
- `AttachmentStorageViewModel`
- `MessageAttachmentViewer`
