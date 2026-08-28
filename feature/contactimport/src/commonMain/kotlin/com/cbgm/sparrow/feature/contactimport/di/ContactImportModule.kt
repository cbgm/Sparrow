package com.cbgm.sparrow.feature.contactimport.di

import com.cbgm.sparrow.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.sparrow.feature.contactimport.domain.usecase.VerifyContactByQrUseCase
import com.cbgm.sparrow.feature.contactimport.presentation.importing.ImportIdentityViewModel
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityNavigationViewModel
import com.cbgm.sparrow.feature.contactimport.presentation.verify.VerifyContactQrViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactImportModule =
    module {
        factory {
            ImportSharedIdentityUseCase(
                identityShareRepository = get(),
                contactRepository = get(),
                identityInvitationRepository = get(),
                identityExchangeRepository = get(),
                deviceContactWriterRepository = get()
            )
        }

        factory {
            VerifyContactByQrUseCase(
                identityShareRepository = get(),
                contactRepository = get(),
                identityInvitationRepository = get(),
                identityExchangeRepository = get(),
                contactVerificationRepository = get()
            )
        }

        viewModel {
            ImportIdentityViewModel(
                savedStateHandle = get(),
                decodeSharedIdentity = get(),
                importSharedIdentity = get()
            )
        }

        viewModel {
            ScanIdentityNavigationViewModel(
                savedStateHandle = get()
            )
        }

        viewModel {
            VerifyContactQrViewModel(
                savedStateHandle = get(),
                decodeSharedIdentity = get(),
                verifyContactByQr = get()
            )
        }
    }
