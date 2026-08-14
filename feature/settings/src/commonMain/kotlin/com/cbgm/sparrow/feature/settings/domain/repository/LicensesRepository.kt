package com.cbgm.sparrow.feature.settings.domain.repository

interface LicensesRepository {
    suspend fun getLibraries(): String
}
