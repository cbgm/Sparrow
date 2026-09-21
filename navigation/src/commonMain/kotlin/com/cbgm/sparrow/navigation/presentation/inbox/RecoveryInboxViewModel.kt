package com.cbgm.sparrow.navigation.presentation.inbox

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.invite.presentation.model.MailboxReviewRequestUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecoveryInboxViewModel(
    observeRequests: ObservePendingRemoteIdentityChangesUseCase,
    private val getPeerDisplayName: GetIdentityPeerDisplayNameUseCase,
    private val getContact: GetContactUseCase
) : BaseViewModel() {
    private val _requests = MutableStateFlow<List<MailboxReviewRequestUi>>(emptyList())
    val requests: StateFlow<List<MailboxReviewRequestUi>> = _requests.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            observeRequests().collect { candidates ->
                _requests.value = candidates.map { candidate ->
                    MailboxReviewRequestUi(
                        peerId = candidate.peerId,
                        invitationId = candidate.invitationId,
                        peerDisplayName = getPeerDisplayName(candidate.peerId)
                            ?.takeIf { it.isNotBlank() }
                            ?: getContact(candidate.peerId).getOrNull()?.preferredPhoneNumber?.value
                                ?.takeIf { it.isNotBlank() }
                            ?: "Contact"
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun review(peerId: String, invitationId: String) {
        navigator.navigateTo(AppRoute.IdentityRecovery(peerId, invitationId))
    }
}
