package com.cbgm.securechat.feature.contactimport.di

import com.cbgm.securechat.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.securechat.feature.contactimport.domain.usecase.VerifyContactByQrUseCase
import com.cbgm.securechat.feature.contactimport.presentation.importing.ImportIdentityViewModel
import com.cbgm.securechat.feature.contactimport.presentation.scan.ScanIdentityNavigationViewModel
import com.cbgm.securechat.feature.contactimport.presentation.verify.VerifyContactQrViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactImportModule =
    module {

        factory {
            ImportSharedIdentityUseCase(
                identityShareRepository = get(),
                importContact = get()
            )
        }

        factory {
            VerifyContactByQrUseCase(
                identityShareRepository = get(),
                importContact = get(),
                verifyContact = get()
            )
        }

        viewModel { parameters ->
            ImportIdentityViewModel(
                route = parameters.get(),
                importSharedIdentity = get()
            )
        }

        viewModel { parameters ->
            ScanIdentityNavigationViewModel(
                route = parameters.get()
            )
        }

        viewModel { parameters ->
            VerifyContactQrViewModel(
                contactId = parameters.get(),
                verifyContactByQr = get()
            )
        }
    }
