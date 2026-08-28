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
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityInvitationRepositoryImpl
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeleteDeclinedOutgoingInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactInviteAcceptedPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactInviteDeclinedPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactInvitePacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactReadyPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactVerificationReceiptPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleDirectChatAuthorizationRevokedPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityAcknowledgementPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityPacketUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.MarkContactInvitationsViewedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveBlockedContactsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactDetailsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactInvitationsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactInvitationsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePictureUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsWithProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObservePendingContactInvitationCountUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObservePendingContactInvitationsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.invitations.ContactInvitationViewModel
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsViewModel
import com.cbgm.sparrow.feature.contacts.util.ContactVerificationPayloadEncoder
import com.cbgm.sparrow.feature.contacts.util.IdentityInvitationPayloadEncoder
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
            IdentityInvitationRepositoryImpl(
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

        single<IdentityInvitationRepository> {
            get<IdentityInvitationRepositoryImpl>()
        }

        factory {
            HandleContactInvitePacketUseCase(
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { HandleContactInviteAcceptedPacketUseCase(identityInvitationRepository = get()) }
        factory { HandleContactReadyPacketUseCase(identityInvitationRepository = get()) }
        factory { HandleContactInviteDeclinedPacketUseCase(identityInvitationRepository = get()) }
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
            AcceptContactInvitationUseCase(
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { DeclineContactInvitationUseCase(identityInvitationRepository = get()) }
        factory {
            DeclineAndBlockContactInvitationUseCase(
                identityInvitationRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { DeleteDeclinedOutgoingInvitationUseCase(repository = get()) }
        factory { MarkContactInvitationsViewedUseCase(repository = get()) }
        factory {
            ObserveContactInvitationsUseCase(
                repository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            RequireDirectChatAuthorizationUseCase(
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory {
            ObservePendingContactInvitationsUseCase(
                identityInvitationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }
        factory { ObservePendingContactInvitationCountUseCase(observePendingContactInvitations = get()) }
        factory { ObserveIdentityHandshakeStateUseCase(identityInvitationRepository = get()) }
        factory { ObserveIdentitySetupModeUseCase(repository = get()) }
        factory { ObserveContactProfilePictureUseCase(provider = get()) }
        factory { ObserveContactProfilePicturesUseCase(provider = get()) }
        factory {
            ObserveContactDetailsContextUseCase(
                observeContact = get(),
                observeProfilePicture = get(),
                getContactSafetyNumber = get()
            )
        }
        factory {
            ObserveBlockedContactsContextUseCase(
                observeContactBlocklist = get(),
                observeProfilePictures = get()
            )
        }
        factory { ObserveContactsWithProfilePicturesUseCase(observeContacts = get(), observeProfilePictures = get()) }
        factory { ObserveContactInvitationsContextUseCase(observeContactInvitations = get(), observeProfilePictures = get()) }
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
                observeContactsWithProfilePictures = get(),
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
