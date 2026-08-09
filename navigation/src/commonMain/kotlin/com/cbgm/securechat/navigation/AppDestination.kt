package com.cbgm.securechat.navigation

import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerType
import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data class Details(
        val child: DetailsChild,
        val conversationId: String,
        val contactId: String? = null,
        val openVerification: Boolean = false
    ) : AppDestination

    @Serializable
    data class GroupConversation(
        val conversationId: String
    ) : AppDestination

    @Serializable
    data object ShareIdentity : AppDestination

    @Serializable
    data class Chat(
        val conversationId: String,
        val contactId: String,
        val contactName: String
    ) : AppDestination

    @Serializable
    data class Disclaimer(
        val type: DisclaimerType
    ) : AppDestination

    @Serializable
    data object Licences : AppDestination

    @Serializable
    data object DeveloperMenu : AppDestination

    @Serializable
    data object ControlPlanes : AppDestination

    @Serializable
    data object BlockedContacts : AppDestination

    @Serializable
    data object Main : AppDestination

    @Serializable
    data object ScanIdentity : AppDestination

    @Serializable
    data class VerifyIdentityQr(
        val contactId: String,
        val groupId: String? = null
    ) : AppDestination

    @Serializable
    data object Startup : AppDestination

    @Serializable
    data class ImportContact(
        val scannedIdentity: String? = null,
        val contactId: String? = null
    ) : AppDestination
}

@Serializable
enum class DetailsChild {
    CONTACT,
    GROUP
}
