package com.cbgm.securechat.feature.settings.data.repository

import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.feature.settings.data.datasource.SettingsStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContactBlocklistRepositoryImpl(
    private val settingsStorage: SettingsStorage
) : ContactBlocklistRepository {
    private val mutex = Mutex()
    private val blockUnknownContactInvites = MutableStateFlow(false)
    private val blockedContactIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeBlockUnknownContactInvites(): Flow<Boolean> =
        blockUnknownContactInvites
            .onStart {
                blockUnknownContactInvites.value = settingsStorage.getBlockUnknownContactInvites()
            }.distinctUntilChanged()

    override suspend fun getBlockUnknownContactInvites(): Boolean {
        val enabled = settingsStorage.getBlockUnknownContactInvites()
        blockUnknownContactInvites.value = enabled
        return enabled
    }

    override suspend fun setBlockUnknownContactInvites(enabled: Boolean) {
        settingsStorage.setBlockUnknownContactInvites(enabled)
        blockUnknownContactInvites.value = enabled
    }

    override fun observeBlockedContactIds(): Flow<Set<String>> =
        blockedContactIds
            .onStart {
                blockedContactIds.value = loadBlockedContactIds()
            }.distinctUntilChanged()

    override suspend fun getBlockedContactIds(): Set<String> {
        val contactIds = loadBlockedContactIds()
        blockedContactIds.value = contactIds
        return contactIds
    }

    override suspend fun isBlocked(contactId: String): Boolean {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        return getBlockedContactIds().contains(contactId)
    }

    override suspend fun block(contactId: String) {
        updateBlockedContactIds(contactId = contactId, blocked = true)
    }

    override suspend fun unblock(contactId: String) {
        updateBlockedContactIds(contactId = contactId, blocked = false)
    }

    private suspend fun updateBlockedContactIds(
        contactId: String,
        blocked: Boolean
    ) {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        mutex.withLock {
            val updatedIds =
                loadBlockedContactIds()
                    .toMutableSet()
                    .apply {
                        if (blocked) {
                            add(contactId)
                        } else {
                            remove(contactId)
                        }
                    }.toSet()

            settingsStorage.setBlockedContactIds(updatedIds.sorted().joinToString(separator = CONTACT_ID_SEPARATOR))
            blockedContactIds.value = updatedIds
        }
    }

    private suspend fun loadBlockedContactIds(): Set<String> =
        settingsStorage
            .getBlockedContactIds()
            ?.split(CONTACT_ID_SEPARATOR)
            ?.map { contactId -> contactId.trim() }
            ?.filter { contactId -> contactId.isNotEmpty() }
            ?.toSet()
            .orEmpty()

    private companion object {
        const val CONTACT_ID_SEPARATOR = "\n"
    }
}
