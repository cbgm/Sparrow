package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.feature.settings.domain.repository.LicensesRepository
import com.cbgm.sparrow.feature.settings.resources.Res

class LicencesRepositoryImpl : LicensesRepository {
    override suspend fun getLibraries(): String = Res.readBytes(path = "files/aboutlibraries.json").decodeToString()
}
