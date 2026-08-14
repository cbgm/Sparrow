package com.cbgm.securechat.core.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Startup : AppRoute

    @Serializable
    data object Main : AppRoute

    @Serializable
    data class Chat(
        val conversationId: String,
        val contactId: String,
        val contactName: String
    ) : AppRoute

    @Serializable
    data class GroupConversation(
        val conversationId: String
    ) : AppRoute

    @Serializable
    data class ContactDetails(
        val conversationId: String,
        val contactId: String,
        val openVerification: Boolean = false
    ) : AppRoute

    @Serializable
    data class GroupDetails(
        val conversationId: String,
        val requestLeave: Boolean = false
    ) : AppRoute

    @Serializable
    data class VerifyIdentityQr(
        val contactId: String,
        val groupId: String? = null
    ) : AppRoute

    @Serializable
    data object ShareIdentity : AppRoute

    @Serializable
    data class ImportContact(
        val scannedIdentity: String? = null,
        val contactId: String? = null
    ) : AppRoute

    @Serializable
    data class ScanIdentity(
        val contactId: String? = null,
        val previousScannedIdentity: String? = null
    ) : AppRoute

    @Serializable
    data object PrivacyPolicy : AppRoute

    @Serializable
    data object DataDisclaimer : AppRoute

    @Serializable
    data object Licenses : AppRoute

    @Serializable
    data object DeveloperMenu : AppRoute

    @Serializable
    data object ControlPlanes : AppRoute

    @Serializable
    data object BlockedContacts : AppRoute

    @Serializable
    data object ContactInvitations : AppRoute
}
