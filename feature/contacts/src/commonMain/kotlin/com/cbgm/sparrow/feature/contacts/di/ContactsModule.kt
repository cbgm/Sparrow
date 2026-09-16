package com.cbgm.sparrow.feature.contacts.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.contacts.adapter.ContactInviteAcceptedPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.ContactInviteDeclinedPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.ContactInvitePacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.ContactLocalIdentityChangeHandler
import com.cbgm.sparrow.feature.contacts.adapter.ContactReadyPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.ContactVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.DirectChatAuthorizationRevokedPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.IdentityAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.contacts.adapter.IdentityPacketHandler
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactVerificationDataSource
import com.cbgm.sparrow.feature.contacts.data.repository.ContactKeyExchangeRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.ContactRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.ContactVerificationRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.DirectInvitationRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactVerificationReceiptPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleDirectChatAuthorizationRevokedPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityAcknowledgementPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveBlockedContactsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactDetailsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.invitations.ContactInvitationViewModel
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsViewModel
import com.cbgm.sparrow.feature.contacts.util.ContactVerificationPayloadEncoder
import com.cbgm.sparrow.feature.contacts.util.IdentityInvitationPayloadEncoder
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactsModule =
    module {

        single {
            ContactKeyExchangeDataSource(
                contactDao = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        single<ContactKeyExchangeRepository> {
            ContactKeyExchangeRepositoryImpl(dataSource = get())
        }

        single<LocalIdentityChangeHandler> {
            ContactLocalIdentityChangeHandler(
                localIdentityDataResetter = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        single {
            IdentityInvitationPayloadEncoder()
        }

        single {
            ContactVerificationPayloadEncoder()
        }

        single {
            ContactVerificationDataSource(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                payloadEncoder = get(),
                protocolOutbox = get()
            )
        }

        single<ContactVerificationRepository> {
            ContactVerificationRepositoryImpl(dataSource = get())
        }

        single {
            DirectInvitationRepositoryImpl(
                invitationDao = get(),
                contactDao = get(),
                contactRoutingIdDao = get(),
                contactKeyExchangeDataSource = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                secureRandomGenerator = get(),
                payloadEncoder = get(),
                protocolOutbox = get(),
                localPhoneNumberProvider = get(),
                phoneNumberNormalizer = get(),
                contactVerificationDataSource = get(),
                localProfilePictureMetadataProvider = get(),
                remoteProfilePictureMetadataProcessor = get()
            )
        }

        single<DirectInvitationRepository> {
            get<DirectInvitationRepositoryImpl>()
        }
        single<InvitationRepository> {
            get<DirectInvitationRepositoryImpl>()
        }

        factory {
            HandleDirectChatAuthorizationRevokedPacketUseCase(
                identityInvitationRepository = get(),
                mailboxCapabilityLifecycle = get()
            )
        }
        factory { HandleContactVerificationReceiptPacketUseCase(contactVerificationRepository = get()) }
        factory {
            HandleIdentityPacketUseCase(
                contactRepository = get(),
                contactKeyExchangeRepository = get(),
                localSigningKeyPairProvider = get(),
                identityAcknowledgementCrypto = get(),
                protocolOutbox = get(),
                contactVerificationRepository = get()
            )
        }
        factory {
            HandleIdentityAcknowledgementPacketUseCase(
                contactRepository = get(),
                contactKeyExchangeRepository = get(),
                localPublicIdentityProvider = get(),
                identityAcknowledgementCrypto = get(),
                contactVerificationRepository = get()
            )
        }

        singleOf(::ContactInvitePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactInviteAcceptedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactReadyPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactInviteDeclinedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::DirectChatAuthorizationRevokedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactVerificationReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::IdentityPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::IdentityAcknowledgementPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<IdentityExchangeRepository> {
            IdentityExchangeRepositoryImpl(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                protocolOutbox = get()
            )
        }

        single<ContactRepository> {
            ContactRepositoryImpl(
                contactDao = get(),
                contactKeyExchangeDataSource = get(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        factory {
            AddDeviceContactUseCase(repository = get())
        }

        factory {
            ImportContactUseCase(
                contactRepository = get(),
                identityInvitationRepository = get(),
                identityExchangeRepository = get(),
                deviceContactWriterRepository = get()
            )
        }

        factory {
            GetContactUseCase(repository = get())
        }

        factory {
            GetContactSafetyNumberUseCase(
                localPublicIdentityProvider = get(),
                contactRepository = get(),
                safetyNumberGenerator = get()
            )
        }

        factory {
            ObserveContactUseCase(repository = get())
        }

        factory {
            ObserveContactsUseCase(repository = get())
        }

        factory {
            ImportDeviceContactsUseCase(
                deviceContactsRepository = get(),
                repository = get()
            )
        }

        factory {
            ObserveContactBlocklistUseCase(
                observeContacts = get(),
                repository = get()
            )
        }

        factory {
            BlockContactUseCase(
                blocklistRepository = get(),
                contactRepository = get(),
                identityInvitationRepository = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        factory {
            UnblockContactUseCase(repository = get())
        }

        factory {
            VerifyContactUseCase(
                repository = get(),
                contactVerificationRepository = get()
            )
        }

        factory {
            RequireDirectChatAuthorizationUseCase(
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { ObserveIdentitySetupModeUseCase(repository = get()) }
        factory {
            ObserveContactDetailsContextUseCase(
                observeContact = get(),
                getContactSafetyNumber = get()
            )
        }
        factory {
            ObserveBlockedContactsContextUseCase(
                observeContactBlocklist = get()
            )
        }
        factory {
            EnsureIdentityExchangeStartedUseCase(
                modeRepository = get(),
                contactBlocklistRepository = get(),
                identityInvitationRepository = get(),
                identityExchangeRepository = get()
            )
        }

        viewModel {
            ContactInvitationViewModel(
                savedStateHandle = get(),
                observeInvitationsContext = get(),
                acceptContactInvitation = get(),
                declineContactInvitation = get(),
                declineAndBlockContactInvitation = get(),
                deleteDeclinedOutgoingInvitation = get(),
                markInvitationsViewed = get()
            )
        }

        viewModel {
            BlockedContactsViewModel(
                savedStateHandle = get(),
                observeBlockedContactsContext = get(),
                blockContact = get(),
                unblockContact = get()
            )
        }

        viewModel {
            ContactsViewModel(
                savedStateHandle = get(),
                observeContacts = get(),
                importDeviceContacts = get()
            )
        }

        viewModel {
            ContactDetailsViewModel(
                savedStateHandle = get(),
                observeContactDetailsContext = get(),
                verifyContact = get()
            )
        }
    }
