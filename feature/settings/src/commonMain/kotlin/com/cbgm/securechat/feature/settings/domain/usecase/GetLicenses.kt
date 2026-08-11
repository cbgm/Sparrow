package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository

class GetLicenses(
    private val repository: LicensesRepository
) {
    suspend operator fun invoke(): String = repository.getLibraries()
}
