package com.cbgm.sparrow.feature.avatar.presentation.model

/** The trust ring around an avatar: dashed while the identity is not verified both ways, solid when it is. */
enum class AvatarRing {
    NONE,
    PENDING,
    VERIFIED
}
