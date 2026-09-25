package com.cbgm.sparrow.feature.avatar.domain.model

sealed interface AvatarTarget {
    val id: String

    data object LocalUser : AvatarTarget {
        override val id: String = "local-user"
    }

    data class User(
        override val id: String
    ) : AvatarTarget {
        init {
            require(id.isNotBlank()) { "Contact ID must not be blank" }
        }
    }

    data class Group(
        override val id: String
    ) : AvatarTarget {
        init {
            require(id.isNotBlank()) { "Group ID must not be blank" }
        }
    }
}
