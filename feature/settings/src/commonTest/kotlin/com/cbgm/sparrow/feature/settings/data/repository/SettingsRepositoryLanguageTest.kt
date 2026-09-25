package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.settings.data.datasource.InMemorySettingsStorage
import com.cbgm.sparrow.feature.settings.device.BuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.SystemLanguageProvider
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryLanguageTest {
    private val buildInfoProvider =
        object : BuildInfoProvider {
            override val build = BuildInfo("test", 1, "test", null)
        }

    @Test
    fun firstStartUsesPhoneLanguageAndPersistsIt() = runBlocking {
        val storage = InMemorySettingsStorage()
        val repository = repository(storage, "de-DE")

        assertEquals(AppLanguage.GERMAN, repository.getLanguage())
        assertEquals("de", storage.getLanguageTag())
        // A later change to the phone locale must not replace the saved choice.
        assertEquals(AppLanguage.GERMAN, repository(storage, "en-US").getLanguage())
    }

    @Test
    fun savedLanguageWinsWithoutReadingPhoneSettings() = runBlocking {
        val storage = InMemorySettingsStorage()
        storage.setLanguageTag("en")
        val repository =
            SettingsRepositoryImpl(
                buildInfoProvider = buildInfoProvider,
                settingsStorage = storage,
                systemLanguageProvider =
                    object : SystemLanguageProvider {
                        override fun getLanguageTag(): String =
                            error("Phone locale should not be read when language is saved")
                    }
            )

        assertEquals(AppLanguage.ENGLISH, repository.getLanguage())
        assertEquals("en", storage.getLanguageTag())
    }

    @Test
    fun unsupportedPhoneLanguageFallsBackToEnglishAndPersistsIt() = runBlocking {
        val storage = InMemorySettingsStorage()

        assertEquals(AppLanguage.ENGLISH, repository(storage, "fr-FR").getLanguage())
        assertEquals("en", storage.getLanguageTag())
    }

    private fun repository(
        storage: InMemorySettingsStorage,
        phoneLanguageTag: String
    ): SettingsRepositoryImpl =
        SettingsRepositoryImpl(
            buildInfoProvider = buildInfoProvider,
            settingsStorage = storage,
            systemLanguageProvider =
                object : SystemLanguageProvider {
                    override fun getLanguageTag(): String = phoneLanguageTag
                }
        )
}
