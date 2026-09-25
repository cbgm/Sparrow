package com.cbgm.sparrow.feature.attachments.domain.model

sealed interface AttachmentContent {
    val target: AttachmentTarget

    data class LocalFile(
        override val target: AttachmentTarget,
        val localFilePath: String
    ) : AttachmentContent {
        init {
            require(localFilePath.isNotBlank()) { "Local attachment path must not be blank" }
        }
    }

    data class Location(
        override val target: AttachmentTarget,
        val location: CurrentLocation
    ) : AttachmentContent

    data class Contact(
        override val target: AttachmentTarget,
        val contact: SharedContact
    ) : AttachmentContent
}
