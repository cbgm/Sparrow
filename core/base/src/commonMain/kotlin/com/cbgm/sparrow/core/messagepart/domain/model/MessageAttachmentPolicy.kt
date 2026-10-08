package com.cbgm.sparrow.core.messagepart.domain.model

object MessageAttachmentPolicy {
    const val MAX_ATTACHMENTS_PER_MESSAGE = 8
    const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    const val MAX_VIDEO_BYTES = 64L * 1024L * 1024L
    const val MAX_FILE_BYTES = 96L * 1024L * 1024L
    const val MAX_TOTAL_ATTACHMENT_BYTES = 96L * 1024L * 1024L
    const val MAX_IMAGE_DIMENSION = 2048
    const val DEFAULT_RETENTION_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L

    fun requireValid(parts: List<MessagePart>) {
        require(parts.size <= MAX_ATTACHMENTS_PER_MESSAGE) {
            "A message can contain at most $MAX_ATTACHMENTS_PER_MESSAGE attachments"
        }
        require(parts.map(MessagePart::id).distinct().size == parts.size) {
            "Message part IDs must be unique"
        }

        require(parts.none { it is Poll } || (parts.size == 1 && parts.single() is Poll)) {
            "A poll cannot contain other attachment parts"
        }
        require(parts.none { it is Expense || it is ExpenseBoard } || parts.size == 1) {
            "An expense or expense board must be a standalone group message"
        }
        val voiceCount = parts.count { it is Voice }
        require(voiceCount == 0 || (voiceCount == 1 && parts.size == 1)) {
            "A voice message cannot contain other attachments"
        }

        parts.forEach { part ->
            require(part.id.isNotBlank()) { "Message part ID must not be blank" }
            when (part) {
                is Image -> {
                    val width = part.width
                    val height = part.height
                    require(part.mimeType.startsWith("image/"))
                    require(width != null && width > 0)
                    require(height != null && height > 0)
                }

                is Video -> {
                    val width = part.width
                    val height = part.height
                    val durationMilliseconds = part.durationMilliseconds
                    require(part.mimeType.startsWith("video/"))
                    require((width == null) == (height == null))
                    require(width == null || width > 0)
                    require(height == null || height > 0)
                    require(durationMilliseconds == null || durationMilliseconds >= 0L)
                }

                is File -> {
                    require(part.mimeType.isNotBlank())
                    require(part.fileName.isNotBlank())
                }

                is Voice -> {
                    require(part.mimeType.startsWith("audio/"))
                    require(part.durationMilliseconds >= 0L)
                }

                is Location,
                is Contact -> Unit

                is Text -> error("Text is not an attachment payload")
                is Poll -> PollPolicy.requireValid(part)
                is Expense -> ExpensePolicy.requireValid(part)
                is ExpenseBoard -> ExpensePolicy.requireValid(part)
            }
        }
    }

    fun requireValidPayload(part: MessagePart, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Message part payload must not be empty" }
        when (part) {
            is Image -> require(bytes.size <= MAX_IMAGE_BYTES)
            is Video -> require(bytes.size.toLong() <= MAX_VIDEO_BYTES)
            is File -> require(bytes.size.toLong() <= MAX_FILE_BYTES)
            is Voice,
            is Location,
            is Contact -> Unit
            is Text -> error("Text is not an attachment payload")
            is Poll -> error("Poll is a structured message part")
            is Expense, is ExpenseBoard -> error("Expenses are structured message parts")
        }
    }

    fun requireValidTotalPayloadBytes(totalBytes: Long) {
        require(totalBytes <= MAX_TOTAL_ATTACHMENT_BYTES) {
            "Selected attachments exceed the total attachment size limit"
        }
    }
}
