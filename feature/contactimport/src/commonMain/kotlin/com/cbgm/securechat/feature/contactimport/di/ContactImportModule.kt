package com.cbgm.securechat.feature.contactimport.di

import com.cbgm.securechat.feature.contactimport.domain.usecase.ImportSharedIdentity
import com.cbgm.securechat.feature.contactimport.domain.usecase.VerifyContactByQr
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityViewModel
import com.cbgm.securechat.feature.contactimport.presentation.screen.ScanIdentityNavigationViewModel
import com.cbgm.securechat.feature.contactimport.presentation.screen.VerifyContactQrViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactImportModule =
    module {

        factory {
            ImportSharedIdentity(
                identityShareCodec = get(),
                importContact = get()
            )
        }

        factory {
            VerifyContactByQr(
                identityShareCodec = get(),
                importContact = get(),
                verifyContact = get()
            )
        }

        viewModel {
            ImportIdentityViewModel(
                importSharedIdentity = get()
            )
        }

        viewModel { ScanIdentityNavigationViewModel() }

        viewModel { parameters ->
            VerifyContactQrViewModel(
                contactId = parameters.get(),
                verifyContactByQr = get()
            )
        }
    }
