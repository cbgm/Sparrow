package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.identity.data.IdentityLocalResetHandler
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityExchangeStoreDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityVerificationDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.LocalIdentityProfileDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.LocalProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.ManualIdentityExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteProfilePictureDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.SparrowDataStorePublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.protocol.IdentityVerificationReceiptEncoder
import com.cbgm.sparrow.feature.identity.data.repository.IdentityExchangeRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.IdentityRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.IdentityVerificationRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalIdentityProfileRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteIdentityImportRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteIdentityReadRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityVerificationRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityImportRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ApplyRemoteProfilePictureMetadataUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CloseIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclineIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EnsureRemoteSigningIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeBindingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.HandleIdentityVerificationReceiptUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ImportRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.InvalidateIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityResultsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveRemoteIdentitiesUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReassignIdentityExchangePeerUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityAcknowledgementUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeAcceptedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveManualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecordRemoteIdentityDeclineUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SendIdentityVerificationReceiptUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.VerifyRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {
        single<LocalIdentityChangeHandler> {
            IdentityLocalResetHandler(
                mailboxCapabilityLifecycle = get(),
                localIdentityDataResetter = get()
            )
        }
        singleOf(::IdentityExchangeStoreDataSource)
        singleOf(::IdentityExchangeDataSource)
        singleOf(::ManualIdentityExchangeDataSource)
        singleOf(::IdentityExchangeRepositoryImpl) { bind<IdentityExchangeRepository>() }
        singleOf(::RemoteIdentityDataSource)
        singleOf(::RemoteIdentityReadRepositoryImpl) { bind<RemoteIdentityReadRepository>() }
        factory { GetRemoteIdentityUseCase(repository = get()) }
        factory { FindRemoteIdentityPeerIdUseCase(repository = get()) }
        factory { ObserveRemoteIdentitiesUseCase(repository = get()) }
        single<RemoteIdentityImportRepository> {
            RemoteIdentityImportRepositoryImpl(remoteIdentityDao = get(), mailboxCapabilityLifecycle = get())
        }
        factory { ImportRemoteIdentityUseCase(repository = get()) }
        singleOf(::IdentityVerificationReceiptEncoder)
        singleOf(::IdentityVerificationDataSource)
        singleOf(::IdentityVerificationRepositoryImpl) { bind<IdentityVerificationRepository>() }
        factory { VerifyRemoteIdentityUseCase(repository = get()) }
        factory { SendIdentityVerificationReceiptUseCase(repository = get()) }
        factory { HandleIdentityVerificationReceiptUseCase(contactVerificationRepository = get()) }

        factory { StartIdentityExchangeUseCase(repository = get()) }
        factory { StartManualIdentityExchangeUseCase(repository = get()) }
        factory { AcceptIdentityExchangeUseCase(repository = get()) }
        factory { DeclineIdentityExchangeUseCase(repository = get()) }
        factory { ObserveIdentityResultsUseCase(repository = get()) }
        factory { CancelIdentityExchangeUseCase(repository = get()) }
        factory { GetIdentityPeerStateUseCase(repository = get()) }
        factory { GetIdentityExchangeBindingUseCase(repository = get()) }
        factory { InvalidateIdentityExchangeUseCase(repository = get()) }
        factory { GetIdentityExchangeClosureUseCase(repository = get()) }
        factory { CloseIdentityExchangeUseCase(repository = get()) }
        factory { ReceiveIdentityExchangeUseCase(repository = get()) }
        factory { ReceiveManualIdentityUseCase(repository = get()) }
        factory { ReceiveIdentityAcknowledgementUseCase(repository = get()) }
        factory { ReassignIdentityExchangePeerUseCase(repository = get()) }
        factory { ReceiveIdentityExchangeAcceptedUseCase(repository = get()) }
        factory { RecordRemoteIdentityDeclineUseCase(repository = get()) }
        factory { ReceiveIdentityReadyUseCase(repository = get()) }
        factory { StageRemoteIdentityUseCase(repository = get()) }
        factory { AcceptRemoteIdentityHandshakeUseCase(repository = get()) }
        factory { EstablishMutualIdentityUseCase(repository = get()) }
        factory { EnsureRemoteSigningIdentityUseCase(repository = get()) }
        factory { ApplyRemoteProfilePictureMetadataUseCase(processor = get()) }

        single<PublicIdentityDataSource> {
            SparrowDataStorePublicIdentityDataSource(dataStore = get())
        }

        single {
            LocalIdentityProfileDataSource(
                dataStore = get(),
                phoneNumberNormalizer = get()
            )
        }

        single<LocalPhoneNumberProvider> {
            get<LocalIdentityProfileDataSource>()
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

        single<LocalProfilePictureMetadataProvider> { get<LocalProfilePictureDataSource>() }
        single<LocalProfilePictureProvider> { get<LocalProfilePictureDataSource>() }
        single<RemoteProfilePictureMetadataProcessor> { get<RemoteProfilePictureDataSource>() }
        single<RemoteProfilePictureProvider> { get<RemoteProfilePictureDataSource>() }

        single {
            IdentityRepositoryImpl(
                identityKeyGenerator = get(),
                signatureCrypto = get(),
                privateKeyStorage = get(),
                publicIdentityDataSource = get()
            )
        }

        single<IdentityRepository> { get<IdentityRepositoryImpl>() }
        single<LocalSigningKeyPairProvider> { get<IdentityRepositoryImpl>() }
        single<LocalPublicIdentityProvider> { get<IdentityRepositoryImpl>() }
        single<LocalEncryptionKeyPairProvider> { get<IdentityRepositoryImpl>() }
        single<LocalSigningPublicKeyProvider> { get<IdentityRepositoryImpl>() }

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

        factory {
            ObserveLocalIdentityReadyUseCase(
                identityRepository = get(),
                localIdentityProfileRepository = get()
            )
        }

        factory {
            ObserveIdentityHandshakeStateUseCase(identityExchangeRepository = get())
        }

        single {
            NormalizeLocalPhoneNumberUseCase(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single {
            SaveLocalPhoneNameUseCase(localIdentityProfileRepository = get<LocalIdentityProfileRepository>())
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
