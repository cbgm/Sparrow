package com.cbgm.sparrow.feature.identity.domain.model

/** What startup does with the local identity. */
enum class IdentityResolution {
    READY,

    /** No identity yet (also after an interrupted setup that had nothing to lose): onboarding. */
    NOT_CREATED,

    /** The keys are lost for good while chats exist: the user restores a backup or resets. */
    KEYS_UNUSABLE
}
