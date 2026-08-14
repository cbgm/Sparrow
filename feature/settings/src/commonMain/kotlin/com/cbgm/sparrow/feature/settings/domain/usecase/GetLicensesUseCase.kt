package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.repository.LicensesRepository

class GetLicensesUseCase(
    private val repository: LicensesRepository
) {
    suspend operator fun invoke(): String = repository.getLibraries()
}
