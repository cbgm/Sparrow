package com.cbgm.sparrow.feature.identity.di

import com.cbgm.sparrow.feature.identity.data.datasource.ProfilePictureFileDataSource
import com.cbgm.sparrow.feature.identity.data.repository.FirebasePhoneVerificationRepository
import com.cbgm.sparrow.feature.identity.data.repository.FirebaseProject
import com.cbgm.sparrow.feature.identity.device.AndroidIdentityBackupCodec
import com.cbgm.sparrow.feature.identity.device.AndroidPrivateKeyStorage
import com.cbgm.sparrow.feature.identity.device.DebugSmsNotifier
import com.cbgm.sparrow.feature.identity.device.IdentityBackupCodec
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidIdentityStorageModule =
    module {
        single<IdentityBackupCodec> { AndroidIdentityBackupCodec() }
        single {
            ProfilePictureFileDataSource(
                rootDirectory = androidContext().filesDir.absolutePath
            )
        }

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(dataStore = get())
        }

        single {
            FirebasePhoneVerificationRepository(
                context = androidContext(),
                firebaseProject =
                    FirebaseProject.of(
                        appId = getPropertyOrNull<String>(FIREBASE_APP_ID_PROPERTY),
                        apiKey = getPropertyOrNull<String>(FIREBASE_API_KEY_PROPERTY),
                        projectId = getPropertyOrNull<String>(FIREBASE_PROJECT_ID_PROPERTY)
                    ),
                testSmsCode =
                    getPropertyOrNull<String>(DEBUG_SMS_CODE_PROPERTY)?.takeIf(String::isNotBlank),
                debugSms = DebugSmsNotifier(androidContext())
            )
        }
        single<PhoneVerificationRepository> { get<FirebasePhoneVerificationRepository>() }
    }
