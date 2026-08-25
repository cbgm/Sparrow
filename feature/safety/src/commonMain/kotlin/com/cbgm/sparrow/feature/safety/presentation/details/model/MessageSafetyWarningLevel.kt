package com.cbgm.sparrow.feature.safety.presentation.details.model

enum class MessageSafetyWarningLevel(
    val id: String
) {
    SUSPICIOUS("suspicious"),
    HIGH("high");

    companion object {
        fun fromId(id: String): MessageSafetyWarningLevel? =
            entries.firstOrNull { level -> level.id == id }
    }
}
