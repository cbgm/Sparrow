package com.cbgm.securechat.feature.identity.di

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.data.provider.IdentityLocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.identity.data.provider.IdentityLocalPhoneNumberProvider
import com.cbgm.securechat.feature.identity.data.provider.IdentityLocalPublicIdentityProvider
import com.cbgm.securechat.feature.identity.data.provider.IdentityLocalSigningKeyPairProvider
import com.cbgm.securechat.feature.identity.data.provider.IdentityLocalSigningPublicKeyProvider
import com.cbgm.securechat.feature.identity.data.repository.IdentityRepositoryImpl
import com.cbgm.securechat.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentityUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentityUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.DecodeSharedIdentityUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.GetLocalPhoneNumberUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentityUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.NormalizeLocalPhoneNumberUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.ObserveLocalIdentityReadyUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase
import com.cbgm.securechat.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.securechat.feature.identity.presentation.share.ShareIdentityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {

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
