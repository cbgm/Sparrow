package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

class GetMailboxContactStatesUseCase(
    private val repository: ContactTransportRepository
) {
    suspend operator fun invoke(): List<MailboxContactState> = repository.getMailboxContactStates()
}
