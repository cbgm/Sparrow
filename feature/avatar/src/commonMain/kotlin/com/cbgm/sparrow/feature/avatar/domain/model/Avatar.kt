package com.cbgm.sparrow.feature.avatar.domain.model

class Avatar(
    val target: AvatarTarget,
    val changedAtEpochMilliseconds: Long,
    val bytes: ByteArray?
) {
    init {
        require(changedAtEpochMilliseconds >= 0L) { "Avatar timestamp must not be negative" }
    }
}
