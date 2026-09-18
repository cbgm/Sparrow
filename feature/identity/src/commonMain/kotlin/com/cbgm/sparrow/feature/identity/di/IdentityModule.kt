package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalEncryptionKeyPairProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalPhoneNumberProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalProfilePictureMetadataProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalProfilePictureProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalPublicIdentityProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityLocalSigningPublicKeyProvider
import com.cbgm.sparrow.feature.identity.adapter.IdentityRemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.identity.adapter.IdentityRemoteProfilePictureProvider
import com.cbgm.sparrow.feature.identity.adapter.direct.ContactInviteAcceptedPacketHandler
import com.cbgm.sparrow.feature.identity.adapter.direct.ContactInviteDeclinedPacketHandler
import com.cbgm.sparrow.feature.identity.adapter.direct.ContactInvitePacketHandler
import com.cbgm.sparrow.feature.identity.adapter.direct.ContactReadyPacketHandler
import com.cbgm.sparrow.feature.identity.adapter.direct.DirectChatAuthorizationRevokedPacketHandler
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.ContactVerificationDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.LocalIdentityProfileDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.LocalProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.SparrowDataStorePublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.direct.authorization.DirectAuthorizationPayloadEncoder
import com.cbgm.sparrow.feature.identity.data.direct.datasource.DirectContactIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.direct.datasource.DirectContactRoutingDataSource
import com.cbgm.sparrow.feature.identity.data.direct.datasource.IdentityExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.direct.identity.DirectIdentityExchangeCoordinator
import com.cbgm.sparrow.feature.identity.data.direct.identity.DirectIdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.direct.protocol.DirectInvitationPayloadEncoder
import com.cbgm.sparrow.feature.identity.data.protocol.ContactVerificationPayloadEncoder
import com.cbgm.sparrow.feature.identity.data.repository.IdentityRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalIdentityProfileRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteIdentityHandshakeRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityHandshakeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ApplyRemoteProfilePictureMetadataUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EnsureRemoteSigningIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.AcceptDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.DeclineDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ObserveDirectIdentityResultsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectAuthorizationRevokedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteAcceptedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteDeclinedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.StartDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {
        singleOf(::IdentityExchangeDataSource)
        singleOf(::DirectContactIdentityDataSource)
        singleOf(::DirectContactRoutingDataSource)
        singleOf(::DirectInvitationPayloadEncoder)
        singleOf(::DirectAuthorizationPayloadEncoder)
        singleOf(::DirectIdentityExchangeCoordinator)
        singleOf(::DirectIdentityExchangeRepositoryImpl) { bind<DirectIdentityExchangeRepository>() }

        singleOf(::RemoteIdentityHandshakeRepositoryImpl) { bind<RemoteIdentityHandshakeRepository>() }
        factory { StageRemoteIdentityUseCase(repository = get()) }
        factory { AcceptRemoteIdentityHandshakeUseCase(repository = get()) }
        factory { EstablishMutualIdentityUseCase(repository = get()) }
        factory { EnsureRemoteSigningIdentityUseCase(repository = get()) }
        factory { ApplyRemoteProfilePictureMetadataUseCase(processor = get()) }

        factory { StartDirectInvitationUseCase(repository = get()) }
        factory { AcceptDirectInvitationUseCase(repository = get()) }
        factory { DeclineDirectInvitationUseCase(repository = get()) }
        factory { ObserveDirectIdentityResultsUseCase(repository = get()) }
        factory { ReceiveDirectInviteUseCase(repository = get()) }
        factory { ReceiveDirectInviteAcceptedUseCase(repository = get()) }
        factory { ReceiveDirectInviteDeclinedUseCase(repository = get()) }
        factory { ReceiveDirectReadyUseCase(repository = get()) }
        factory { ReceiveDirectAuthorizationRevokedUseCase(repository = get()) }

        singleOf(::ContactInvitePacketHandler) { bind<TypedProtocolPacketHandler>() }
        singleOf(::ContactInviteAcceptedPacketHandler) { bind<TypedProtocolPacketHandler>() }
        singleOf(::ContactInviteDeclinedPacketHandler) { bind<TypedProtocolPacketHandler>() }
        singleOf(::ContactReadyPacketHandler) { bind<TypedProtocolPacketHandler>() }
        singleOf(::DirectChatAuthorizationRevokedPacketHandler) { bind<TypedProtocolPacketHandler>() }

        single {
            ContactKeyExchangeDataSource(
                contactDao = get(),
                mailboxCapabilityLifecycle = get()
            )
        }

        single { ContactVerificationPayloadEncoder() }

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

        single<PublicIdentityDataSource> {
            SparrowDataStorePublicIdentityDataSource(dataStore = get())
        }

        single {
            LocalIdentityProfileDataSource(dataStore = get())
        }

        single {
            LocalProfilePictureDataSource(
                dataStore = get(),
                fileDataSource = get()
            )
        }

        single {
            RemoteProfilePictureDataSource(
                dataStore = get(),
                fileDataSource = get(),
                cryptoHash = get()
            )
        }

        single<LocalIdentityProfileRepository> {
            LocalIdentityProfileRepositoryImpl(dataSource = get())
        }

        single<LocalProfilePictureRepository> {
            LocalProfilePictureRepositoryImpl(dataSource = get())
        }

        single<RemoteProfilePictureRepository> {
            RemoteProfilePictureRepositoryImpl(dataSource = get())
        }

        single<IdentityRepository> {
            IdentityRepositoryImpl(
                identityKeyGenerator = get(),
                signatureCrypto = get(),
                privateKeyStorage = get(),
                publicIdentityDataSource = get()
            )
        }

        single {
            CreateIdentityUseCase(repository = get<IdentityRepository>())
        }

        single {
            GetIdentityStatusUseCase(repository = get<IdentityRepository>())
        }

        single {
            RecoverIncompleteIdentityUseCase(
                identityRepository = get<IdentityRepository>(),
                localIdentityChangeHandler = get()
            )
        }

        single {
            GetPublicIdentityUseCase(repository = get<IdentityRepository>())
        }

        single {
            GetLocalPhoneNumberUseCase(localIdentityProfileRepository = get<LocalIdentityProfileRepository>())
        }

        factory {
            ObserveLocalProfilePictureUseCase(repository = get<LocalProfilePictureRepository>())
        }

        factory {
            SetLocalProfilePictureUseCase(repository = get<LocalProfilePictureRepository>())
        }

        factory {
            RemoveLocalProfilePictureUseCase(repository = get<LocalProfilePictureRepository>())
        }

        single<LocalProfilePictureMetadataProvider> {
            IdentityLocalProfilePictureMetadataProvider(repository = get())
        }

        single<LocalProfilePictureProvider> {
            IdentityLocalProfilePictureProvider(repository = get())
        }

        single<RemoteProfilePictureMetadataProcessor> {
            IdentityRemoteProfilePictureMetadataProcessor(repository = get())
        }

        single<RemoteProfilePictureProvider> {
            IdentityRemoteProfilePictureProvider(repository = get())
        }

        factory {
            ObserveLocalIdentityReadyUseCase(
                identityRepository = get(),
                localIdentityProfileRepository = get()
            )
        }

        factory {
            ObserveIdentityHandshakeStateUseCase(directIdentityExchangeRepository = get())
        }

        single {
            NormalizeLocalPhoneNumberUseCase(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single {
            SaveLocalPhoneNameUseCase(localIdentityProfileRepository = get<LocalIdentityProfileRepository>())
        }

        single<LocalPhoneNumberProvider> {
            IdentityLocalPhoneNumberProvider(
                localIdentityProfileRepository = get<LocalIdentityProfileRepository>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        single<LocalSigningKeyPairProvider> {
            IdentityLocalSigningKeyPairProvider(identityRepository = get<IdentityRepository>())
        }

        single<LocalPublicIdentityProvider> {
            IdentityLocalPublicIdentityProvider(identityRepository = get<IdentityRepository>())
        }

        single<LocalEncryptionKeyPairProvider> {
            IdentityLocalEncryptionKeyPairProvider(identityRepository = get<IdentityRepository>())
        }

        single<LocalSigningPublicKeyProvider> {
            IdentityLocalSigningPublicKeyProvider(identityRepository = get<IdentityRepository>())
        }

        single<IdentityShareRepository> {
            IdentityShareRepositoryImpl()
        }

        factory {
            DecodeSharedIdentityUseCase(identityShareRepository = get<IdentityShareRepository>())
        }

        factory {
            CreateSharedIdentityUseCase(
                getPublicIdentity = get<GetPublicIdentityUseCase>(),
                localIdentityProfileRepository = get<LocalIdentityProfileRepository>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>(),
                identityShareRepository = get<IdentityShareRepository>()
            )
        }

        viewModel {
            IdentityViewModel(
                savedStateHandle = get(),
                getIdentityStatus = get<GetIdentityStatusUseCase>(),
                getPublicIdentity = get<GetPublicIdentityUseCase>(),
                createIdentity = get<CreateIdentityUseCase>(),
                getLocalPhoneNumber = get<GetLocalPhoneNumberUseCase>(),
                normalizeLocalPhoneNumber = get<NormalizeLocalPhoneNumberUseCase>(),
                saveLocalPhoneName = get<SaveLocalPhoneNameUseCase>()
            )
        }

        viewModel {
            ShareIdentityViewModel(
                createSharedIdentity = get<CreateSharedIdentityUseCase>()
            )
        }
    }
