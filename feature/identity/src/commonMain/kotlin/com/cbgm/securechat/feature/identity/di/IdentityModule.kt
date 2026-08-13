package com.cbgm.securechat.feature.identity.di

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalPhoneNumberProvider
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalPublicIdentityProvider
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalSigningKeyPairProvider
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalSigningPublicKeyProvider
import com.cbgm.securechat.feature.identity.data.repository.DefaultIdentityRepository
import com.cbgm.securechat.feature.identity.data.sharing.DefaultIdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.DecodeSharedIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetLocalPhoneNumber
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.NormalizeLocalPhoneNumber
import com.cbgm.securechat.feature.identity.domain.usecase.ObserveLocalIdentityReady
import com.cbgm.securechat.feature.identity.domain.usecase.RecoverIncompleteIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.SaveLocalPhoneName
import com.cbgm.securechat.feature.identity.presentation.screen.setup.IdentityViewModel
import com.cbgm.securechat.feature.identity.presentation.screen.share.ShareIdentityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {

        single<IdentityRepository> {
            DefaultIdentityRepository(
                identityKeyGenerator = get(),
                signatureCrypto = get(),
                privateKeyStorage = get(),
                publicIdentityStorage = get()
            )
        }

        single {
            CreateIdentity(repository = get<IdentityRepository>())
        }

        single {
            GetIdentityStatus(repository = get<IdentityRepository>())
        }

        single {
            RecoverIncompleteIdentity(
                identityRepository = get<IdentityRepository>(),
                localIdentityChangeHandler = get()
            )
        }

        single {
            GetPublicIdentity(repository = get<IdentityRepository>())
        }

        single {
            GetLocalPhoneNumber(localPhoneNameStorage = get<LocalPhoneNameStorage>())
        }

        factory {
            ObserveLocalIdentityReady(
                identityRepository = get(),
                localPhoneNameStorage = get()
            )
        }

        single {
            NormalizeLocalPhoneNumber(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single {
            SaveLocalPhoneName(localPhoneNameStorage = get<LocalPhoneNameStorage>())
        }

        single<LocalPhoneNumberProvider> {
            IdentityLocalPhoneNumberProvider(
                localPhoneNameStorage = get<LocalPhoneNameStorage>(),
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

        single<IdentityShareCodec> {
            DefaultIdentityShareCodec()
        }

        factory {
            DecodeSharedIdentity(identityShareCodec = get<IdentityShareCodec>())
        }

        factory {
            CreateSharedIdentity(
                getPublicIdentity = get<GetPublicIdentity>(),
                localPhoneNameStorage = get<LocalPhoneNameStorage>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>(),
                identityShareCodec = get<IdentityShareCodec>()
            )
        }

        viewModel {
            IdentityViewModel(
                getIdentityStatus = get<GetIdentityStatus>(),
                getPublicIdentity = get<GetPublicIdentity>(),
                createIdentity = get<CreateIdentity>(),
                getLocalPhoneNumber = get<GetLocalPhoneNumber>(),
                normalizeLocalPhoneNumber = get<NormalizeLocalPhoneNumber>(),
                saveLocalPhoneName = get<SaveLocalPhoneName>()
            )
        }

        viewModel {
            ShareIdentityViewModel(
                createSharedIdentity = get<CreateSharedIdentity>()
            )
        }
    }
