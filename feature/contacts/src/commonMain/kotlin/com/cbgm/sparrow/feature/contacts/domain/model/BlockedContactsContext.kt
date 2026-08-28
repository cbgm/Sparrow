package com.cbgm.sparrow.feature.contacts.domain.model

data class BlockedContactsContext(
    val blocklist: ContactBlocklist,
    val profilePictures: Map<String, ByteArray?>
)
