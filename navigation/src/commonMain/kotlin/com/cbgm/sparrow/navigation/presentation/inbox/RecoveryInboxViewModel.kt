package com.cbgm.sparrow.navigation.presentation.inbox

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ApprovePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclinePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DismissPendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase
import com.cbgm.sparrow.feature.invite.presentation.model.MailboxReviewRequestUi
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Navigation composes an Identity-owned approval use case with Invite's UI-only mailbox rows. */
class RecoveryInboxViewModel(
    observeRequests: ObservePendingRemoteIdentityChangesUseCase,
    private val getPeerDisplayName: GetIdentityPeerDisplayNameUseCase,
    private val getContact: GetContactUseCase,
    private val approveReplacement: ApprovePendingRemoteIdentityChangeUseCase,
    private val dismissReplacement: DismissPendingRemoteIdentityChangeUseCase,
    private val declineReplacement: DeclinePendingRemoteIdentityChangeUseCase,
    private val blockContact: BlockContactUseCase,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val declineGroupInvitation: DeclineInvitationUseCase
) : BaseViewModel() {
    private val _requests = MutableStateFlow<List<MailboxReviewRequestUi>>(emptyList())
    val requests: StateFlow<List<MailboxReviewRequestUi>> = _requests.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _processing = MutableStateFlow<String?>(null)
    val processing: StateFlow<String?> = _processing.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                            ?: "Contact",
                        proposedSigningFingerprint = candidate.proposedSigningPublicKey.toFingerprint(),
                        proposedEncryptionFingerprint = candidate.proposedEncryptionPublicKey.toFingerprint()
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun approve(request: MailboxReviewRequestUi) {
        if (_processing.value != null || _requests.value.none { it == request }) return
        viewModelScope.launch {
            _processing.value = request.invitationId
            _error.value = null
            try {
                approveReplacement(
                    request.peerId,
                    request.invitationId,
                    request.proposedSigningFingerprint,
                    request.proposedEncryptionFingerprint
                ).getOrThrow()
                // No recovery screen, second confirmation, or manual reconnect action.
                // The Identity result and approved-intent observer handle the rest.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _error.value = failure.message ?: "Could not approve identity change"
            } finally {
                _processing.value = null
            }
        }
    }

    fun decline(request: MailboxReviewRequestUi) = perform(request) {
        // An identity proposal attached to a group offer must also close that
        // group invitation, otherwise it would become actionable again as soon
        // as the declined identity-change row disappears.
        declineGroupOfferIfCurrent(request)
        declineReplacement(request.peerId, request.invitationId).getOrThrow()
    }

    fun block(request: MailboxReviewRequestUi) = perform(request) {
        // Block first so that a retransmitted group offer cannot create another
        // actionable proposal. The historical chat and messages are untouched.
        blockContact(request.peerId).getOrThrow()
        declineGroupOfferIfCurrent(request)
        dismissReplacement(request.peerId, request.invitationId).getOrThrow()
    }

    private suspend fun declineGroupOfferIfCurrent(request: MailboxReviewRequestUi) {
        val group = getMembershipHandshake(request.invitationId).getOrThrow() ?: return
        check(group.peerId == request.peerId) { "Group invitation does not belong to this identity" }
        declineGroupInvitation(request.invitationId).getOrThrow()
    }

    private fun perform(request: MailboxReviewRequestUi, action: suspend () -> Unit) {
        if (_processing.value != null || _requests.value.none {
                it === request ||
                    (
                        it.peerId == request.peerId && it.invitationId == request.invitationId &&
                            it.proposedSigningFingerprint == request.proposedSigningFingerprint &&
                            it.proposedEncryptionFingerprint == request.proposedEncryptionFingerprint
                    )
            }
        ) {
            return
        }
        viewModelScope.launch {
            _processing.value = request.invitationId
            _error.value = null
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _error.value = failure.message ?: "Could not update identity invitation"
            } finally {
                _processing.value = null
            }
        }
    }
}
