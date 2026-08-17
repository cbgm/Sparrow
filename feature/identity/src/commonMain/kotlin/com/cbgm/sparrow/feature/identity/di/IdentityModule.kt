package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityStorage
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityStorageImpl
import com.cbgm.sparrow.feature.identity.data.profile.IdentityRemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalEncryptionKeyPairProvider
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalPhoneNumberProvider
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalProfilePictureMetadataProvider
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalPublicIdentityProvider
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.identity.data.provider.IdentityLocalSigningPublicKeyProvider
import com.cbgm.sparrow.feature.identity.data.repository.IdentityRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalIdentityProfileRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.LocalProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteProfilePictureRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CreateSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RemoveLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SetLocalProfilePictureUseCase
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.sparrow.feature.identity.presentation.share.ShareIdentityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {
        single<PublicIdentityStorage> {
            PublicIdentityStorageImpl(dataStore = get())
        }

        single<LocalIdentityProfileRepository> {
            LocalIdentityProfileRepositoryImpl(dataStore = get())
        }

        single<LocalProfilePictureRepository> {
            LocalProfilePictureRepositoryImpl(
                dataStore = get(),
                fileStorage = get()
            )
        }

        single<RemoteProfilePictureRepository> {
            RemoteProfilePictureRepositoryImpl(
                dataStore = get(),
                fileStorage = get(),
                cryptoHash = get()
            )
        }

        single<IdentityRepository> {
            IdentityRepositoryImpl(
                identityKeyGenerator = get(),
                signatureCrypto = get(),
                privateKeyStorage = get(),
                publicIdentityStorage = get()
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

        single<RemoteProfilePictureMetadataProcessor> {
            IdentityRemoteProfilePictureMetadataProcessor(repository = get())
        }

        factory {
            ObserveLocalIdentityReadyUseCase(
                identityRepository = get(),
                localIdentityProfileRepository = get()
            )
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
