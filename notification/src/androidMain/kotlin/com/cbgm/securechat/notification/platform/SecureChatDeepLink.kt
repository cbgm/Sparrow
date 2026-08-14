package com.cbgm.securechat.notification.platform

import android.content.Intent
import android.net.Uri

internal object SecureChatDeepLink {
    private const val SCHEME = "securechat"
    private const val CHAT_HOST = "chat"

    fun conversationUri(conversationId: String): Uri {
        require(conversationId.isNotBlank()) {
            "Conversation ID must not be blank"
        }

        return Uri
            .Builder()
            .scheme(SCHEME)
            .authority(CHAT_HOST)
            .appendPath(conversationId)
            .build()
    }

    fun conversationId(intent: Intent?): String? =
        intent
            ?.takeIf { it.action == Intent.ACTION_VIEW }
            ?.data
            ?.takeIf { uri ->
                uri.scheme == SCHEME && uri.host == CHAT_HOST
            }?.pathSegments
            ?.singleOrNull()
            ?.takeIf(String::isNotBlank)
}
