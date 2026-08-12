package com.cbgm.securechat.feature.contacts.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.data.identity.ContactLocalIdentityChangeHandler
import com.cbgm.securechat.feature.contacts.data.identity.ContactVerificationCoordinator
import com.cbgm.securechat.feature.contacts.data.identity.ContactVerificationPayloadEncoder
import com.cbgm.securechat.feature.contacts.data.identity.DefaultIdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.data.identity.IdentityInvitationCoordinator
import com.cbgm.securechat.feature.contacts.data.identity.IdentityInvitationPayloadEncoder
import com.cbgm.securechat.feature.contacts.data.identity.ManualIdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.data.merge.DefaultContactMergeService
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInviteAcceptedPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInviteDeclinedPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInvitePacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactReadyPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactVerificationReceiptPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.DirectChatAuthorizationRevokedPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.IdentityAcknowledgementPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.IdentityPacketHandler
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactRepository
import com.cbgm.securechat.feature.contacts.domain.identity.ContactVerificationService
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.AcceptContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.BlockContact
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineAndBlockContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.DeclineContactInvitation
import com.cbgm.securechat.feature.contacts.domain.usecase.EnsureIdentityExchangeStarted
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactBlocklist
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentitySetupMode
import com.cbgm.securechat.feature.contacts.domain.usecase.ObservePendingContactInvitationCount
import com.cbgm.securechat.feature.contacts.domain.usecase.ObservePendingContactInvitations
import com.cbgm.securechat.feature.contacts.domain.usecase.UnblockContact
import com.cbgm.securechat.feature.contacts.domain.usecase.VerifyContact
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactInvitationViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.blocklist.BlockedContactsViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.details.ContactDetailsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val contactsModule =
    module {

        single<ContactMergeService> {
            DefaultContactMergeService(
                contactDao = get<ContactDao>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        single<ContactKeyExchangeStore> {
            DefaultContactKeyExchangeStore(
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
            ContactVerificationCoordinator(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                payloadEncoder = get(),
                protocolOutbox = get()
            )
        }

        single<ContactVerificationService> {
            get<ContactVerificationCoordinator>()
        }

        single {
            IdentityInvitationCoordinator(
                invitationDao = get(),
                contactDao = get(),
                contactRelayIdDao = get(),
                contactKeyExchangeStore = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                secureRandomGenerator = get(),
                payloadEncoder = get(),
                protocolOutbox = get(),
                localPhoneNumberProvider = get(),
                phoneNumberNormalizer = get(),
                contactVerificationService = get(),
                modeRepository = get(),
                contactBlocklistRepository = get()
            )
        }

        single<IdentityInvitationService> {
            get<IdentityInvitationCoordinator>()
        }

        single {
            ManualIdentityExchangeStarter(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                protocolOutbox = get(),
                identityInvitationService = get()
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

        single<IdentityExchangeStarter> {
            DefaultIdentityExchangeStarter(
                modeRepository = get(),
                identityInvitationService = get(),
                manualIdentityExchangeStarter = get()
            )
        }

        single<ContactRepository> {
            DefaultContactRepository(
                contactDao = get(),
                mergeService = get(),
                contactKeyExchangeStore = get(),
                identityExchangeStarter = get(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>(),
                deviceContactWriter = get()
            )
        }

        factory {
            ImportContact(repository = get())
        }

        factory {
            GetContact(repository = get())
        }

        factory {
            GetContactSafetyNumber(
                localPublicIdentityProvider = get(),
                contactRepository = get(),
                safetyNumberGenerator = get()
            )
        }

        factory {
            ObserveContact(repository = get())
        }

        factory {
            ObserveContacts(repository = get())
        }

        factory {
            ImportDeviceContacts(
                deviceContactsDataSource = get(),
                repository = get()
            )
        }

        factory {
            ObserveContactBlocklist(
                observeContacts = get(),
                repository = get()
            )
        }

        factory {
            BlockContact(
                blocklistRepository = get(),
                contactRepository = get(),
                identityInvitationService = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        factory {
            UnblockContact(repository = get())
        }

        factory {
            VerifyContact(
                repository = get(),
                contactVerificationService = get()
            )
        }

        factory { AcceptContactInvitation(identityInvitationService = get()) }
        factory { DeclineContactInvitation(identityInvitationService = get()) }
        factory { DeclineAndBlockContactInvitation(identityInvitationService = get()) }
        factory {
            ObservePendingContactInvitations(
                identityInvitationService = get(),
                modeRepository = get()
            )
        }
        factory { ObservePendingContactInvitationCount(observePendingContactInvitations = get()) }
        factory { ObserveIdentityHandshakeState(identityInvitationService = get()) }
        factory { ObserveIdentitySetupMode(repository = get()) }
        factory { EnsureIdentityExchangeStarted(identityExchangeStarter = get()) }

        viewModel {
            ContactInvitationViewModel(
                observePendingContactInvitations = get(),
                acceptContactInvitation = get(),
                declineContactInvitation = get(),
                declineAndBlockContactInvitation = get()
            )
        }

        viewModel {
            BlockedContactsViewModel(
                observeContactBlocklist = get(),
                blockContact = get(),
                unblockContact = get()
            )
        }

        viewModel {
            ContactsViewModel(
                observeContacts = get(),
                importDeviceContacts = get()
            )
        }

        viewModel { parameters ->
            ContactDetailsViewModel(
                contactId = parameters.get(),
                getContact = get(),
                observeContact = get(),
                getContactSafetyNumber = get(),
                verifyContact = get()
            )
        }
    }
