package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.domain.repository.LicensesRepository
import com.cbgm.sparrow.feature.settings.resources.Res

class LicencesRepositoryImpl : LicensesRepository {
    override suspend fun getLibraries(): Result<String> =
        safeSuspendCall { Res.readBytes(path = "files/aboutlibraries.json").decodeToString() }
}
