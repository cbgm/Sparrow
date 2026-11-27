package com.cbgm.sparrow.core.base.group.domain.model

/** Limits of admin-managed group metadata, shared by the editor UI and the orchestration that publishes it. */
object GroupMetadataPolicy {
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_AVATAR_BYTES = 1_048_576

    /** Titles are stored without surrounding whitespace, so " Team " and "Team" are the same title. */
    fun normalizeTitle(title: String): String = title.trim()

    /** Returns the normalized title; a group always needs a visible title. */
    fun requireValidTitle(title: String): String {
        val normalized = normalizeTitle(title)
        require(normalized.isNotEmpty()) { "Group title must not be blank" }
        return normalized
    }

    /** A blank description means the group has none. */
    fun normalizeDescription(description: String): String? = description.trim().takeIf(String::isNotEmpty)

    /** Returns the normalized description, null when it is blank. */
    fun requireValidDescription(description: String): String? {
        val normalized = normalizeDescription(description)
        require(normalized == null || normalized.length <= MAX_DESCRIPTION_LENGTH) {
            "Group description must not exceed $MAX_DESCRIPTION_LENGTH characters"
        }
        return normalized
    }

    /** An avatar is removed explicitly, never by publishing an empty image. */
    fun requireValidAvatar(bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Group avatar must not be empty" }
        require(bytes.size <= MAX_AVATAR_BYTES) { "Group avatar must not exceed $MAX_AVATAR_BYTES bytes" }
    }
}
