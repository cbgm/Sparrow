package com.cbgm.sparrow.feature.contacts.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.contacts.data.exchange.ManualIdentityExchange
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.ContactInviteAcceptedPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.ContactInviteDeclinedPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.ContactInvitePacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.ContactReadyPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.ContactVerificationReceiptPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.DirectChatAuthorizationRevokedPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.IdentityAcknowledgementPacketHandler
import com.cbgm.sparrow.feature.contacts.data.incoming.handler.IdentityPacketHandler
import com.cbgm.sparrow.feature.contacts.data.invitation.IdentityInvitationPayloadEncoder
import com.cbgm.sparrow.feature.contacts.data.merge.ContactMergeService
import com.cbgm.sparrow.feature.contacts.data.merge.ContactMergeServiceImpl
import com.cbgm.sparrow.feature.contacts.data.repository.ContactKeyExchangeRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.ContactRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.ContactVerificationRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.DeviceContactWriterRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.DeviceContactsRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityInvitationRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.verification.ContactLocalIdentityChangeHandler
import com.cbgm.sparrow.feature.contacts.data.verification.ContactVerificationPayloadEncoder
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.AcceptContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineAndBlockContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.DeclineContactInvitationUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePictureUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactProfilePicturesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObservePendingContactInvitationCountUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObservePendingContactInvitationsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.invitations.ContactInvitationViewModel
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val contactsModule =
    module {

        single<DeviceContactsRepository> {
            DeviceContactsRepositoryImpl(dataSource = get())
        }

        single<DeviceContactWriterRepository> {
            DeviceContactWriterRepositoryImpl(dataSource = get())
        }

        single<ContactMergeService> {
            ContactMergeServiceImpl(
                contactDao = get<ContactDao>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        single<ContactKeyExchangeRepository> {
            ContactKeyExchangeRepositoryImpl(
                contactDao = get(),
                mailboxCapabilityLifecycle = get()
            )
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
            ContactVerificationRepositoryImpl(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                payloadEncoder = get(),
                protocolOutbox = get()
            )
        }

        single<ContactVerificationRepository> {
            get<ContactVerificationRepositoryImpl>()
        }

        single {
            IdentityInvitationRepositoryImpl(
                invitationDao = get(),
                contactDao = get(),
                contactRoutingIdDao = get(),
                contactKeyExchangeRepository = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                secureRandomGenerator = get(),
                payloadEncoder = get(),
                protocolOutbox = get(),
                localPhoneNumberProvider = get(),
                phoneNumberNormalizer = get(),
                contactVerificationRepository = get(),
                modeRepository = get(),
                contactBlocklistRepository = get(),
                localProfilePictureMetadataProvider = get(),
                remoteProfilePictureMetadataProcessor = get()
            )
        }

        single<IdentityInvitationRepository> {
            get<IdentityInvitationRepositoryImpl>()
        }

        single {
            ManualIdentityExchange(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                protocolOutbox = get(),
                identityInvitationRepository = get()
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

        single {
            DirectChatAuthorizationRevokedPacketHandler(
                coordinator = get(),
                mailboxCapabilityLifecycle = get()
            )
        }.bind<TypedProtocolPacketHandler>()

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
                modeRepository = get(),
                identityInvitationRepository = get(),
                manualIdentityExchange = get()
            )
        }

        single<ContactRepository> {
            ContactRepositoryImpl(
                contactDao = get(),
                mergeService = get(),
                contactKeyExchangeRepository = get(),
                identityExchangeRepository = get(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>(),
                deviceContactWriterRepository = get()
            )
        }

        factory {
            ImportContactUseCase(repository = get())
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

        factory { AcceptContactInvitationUseCase(identityInvitationRepository = get()) }
        factory { DeclineContactInvitationUseCase(identityInvitationRepository = get()) }
        factory { DeclineAndBlockContactInvitationUseCase(identityInvitationRepository = get()) }
        factory {
            ObservePendingContactInvitationsUseCase(
                identityInvitationRepository = get(),
                modeRepository = get()
            )
        }
        factory { ObservePendingContactInvitationCountUseCase(observePendingContactInvitations = get()) }
        factory { ObserveIdentityHandshakeStateUseCase(identityInvitationRepository = get()) }
        factory { ObserveIdentitySetupModeUseCase(repository = get()) }
        factory { ObserveContactProfilePictureUseCase(provider = get()) }
        factory { ObserveContactProfilePicturesUseCase(provider = get()) }
        factory { EnsureIdentityExchangeStartedUseCase(identityExchangeRepository = get()) }

        viewModel {
            ContactInvitationViewModel(
                observePendingContactInvitations = get(),
                acceptContactInvitation = get(),
                declineContactInvitation = get(),
                declineAndBlockContactInvitation = get(),
                observeProfilePictures = get()
            )
        }

        viewModel {
            BlockedContactsViewModel(
                savedStateHandle = get(),
                observeContactBlocklist = get(),
                blockContact = get(),
                unblockContact = get(),
                observeProfilePictures = get()
            )
        }

        viewModel {
            ContactsViewModel(
                savedStateHandle = get(),
                observeContacts = get(),
                importDeviceContacts = get(),
                observeProfilePictures = get()
            )
        }

        viewModel {
            ContactDetailsViewModel(
                savedStateHandle = get(),
                observeContact = get(),
                getContactSafetyNumber = get(),
                verifyContact = get(),
                observeProfilePicture = get()
            )
        }
    }
