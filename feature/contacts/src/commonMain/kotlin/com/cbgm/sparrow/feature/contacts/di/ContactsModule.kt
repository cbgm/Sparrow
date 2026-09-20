package com.cbgm.sparrow.feature.contacts.di

import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactLocalDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactRoutingIdDataSource
import com.cbgm.sparrow.feature.contacts.data.repository.ContactRepositoryImpl
import com.cbgm.sparrow.feature.contacts.data.repository.IdentityPeerRepositoryImpl
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ImportDeviceContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveBlockedContactsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactBlocklistUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactDetailsContextUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveIncomingPeerContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.UnblockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.ApplyIdentityPeerMergeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.InspectContactPeerUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.UpdateIncomingIdentityPeerMetadataUseCase
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.BlockedContactsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsViewModel
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactsModule =
    module {

        singleOf(::ContactLocalDataSource)
        singleOf(::ContactRoutingIdDataSource)
        singleOf(::IdentityPeerRepositoryImpl) { bind<IdentityPeerRepository>() }
        factory { GetIdentityPeerDisplayNameUseCase(repository = get()) }
        factory { InspectContactPeerUseCase(repository = get()) }
        factory { ApplyIdentityPeerMergeUseCase(repository = get()) }
        factory { UpdateIncomingIdentityPeerMetadataUseCase(repository = get()) }

        single<ContactRepository> {
            ContactRepositoryImpl(
                contactDataSource = get(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        factory {
            AddDeviceContactUseCase(repository = get())
        }

        factory {
            ImportContactUseCase(
                contactRepository = get(),
                cancelIdentityExchange = get(),
                importRemoteIdentity = get(),
                findRemoteIdentityPeerId = get(),
                startManualIdentityExchange = get(),
                deviceContactWriterRepository = get(),
                getContact = get()
            )
        }

        factory {
            GetContactUseCase(repository = get(), getRemoteIdentity = get())
        }

        factory {
            GetContactSafetyNumberUseCase(
                localPublicIdentityProvider = get(),
                contactRepository = get(),
                getRemoteIdentity = get(),
                safetyNumberGenerator = get()
            )
        }

        factory {
            ObserveContactUseCase(observeContacts = get())
        }

        factory {
            ObserveContactsUseCase(repository = get(), observeRemoteIdentities = get())
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
                mailboxCapabilityLifecycle = get()
            )
        }

        factory { ResolveIncomingPeerContactsUseCase(get(), get(), get()) }

        factory {
            UnblockContactUseCase(repository = get())
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
