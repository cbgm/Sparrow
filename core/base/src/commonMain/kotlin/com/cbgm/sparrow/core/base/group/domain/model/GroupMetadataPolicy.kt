package com.cbgm.sparrow.core.base.group.domain.model

/** Limits of admin-managed group metadata, shared by the editor UI and the orchestration that publishes it. */
object GroupMetadataPolicy {
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_AVATAR_BYTES = 1_048_576
}
