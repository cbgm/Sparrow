package com.cbgm.sparrow.feature.onboarding.presentation

import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.ui.theme.currentAppThemeMode
import com.cbgm.sparrow.feature.onboarding.device.PermissionRequestResult
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.settings.data.datasource.InMemorySettingsStorage
import com.cbgm.sparrow.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.sparrow.feature.settings.device.BuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.SystemLanguageProvider
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.domain.usecase.GetAppThemeModeUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetAppThemeModeUseCase
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.background.ConnectionModeStore
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Swiping on past a page does what its action would, once; swiping back does nothing. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val store = RecordingStore()
    private val settingsRepository =
        SettingsRepositoryImpl(
            buildInfoProvider =
                object : BuildInfoProvider {
                    override val build = BuildInfo("test", 1, "test", null)
                },
            settingsStorage = InMemorySettingsStorage(),
            systemLanguageProvider =
                object : SystemLanguageProvider {
                    override fun getLanguageTag(): String = "en"
                }
        )
    private val viewModel by lazy {
        OnboardingViewModel(
            setConnectionMode = SetConnectionModeUseCase(store),
            canConnectWhileClosed = CanConnectWhileClosedUseCase(),
            getAppThemeMode = GetAppThemeModeUseCase(settingsRepository),
            setAppThemeMode = SetAppThemeModeUseCase(settingsRepository)
        )
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun choosingAColorSchemeAppliesAndSavesIt() = runBlocking {
        viewModel.onUiEvent(OnboardingUiEvent.ThemeModeChosen(AppThemeMode.DARK))

        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(AppThemeMode.DARK, currentAppThemeMode.value)
        assertEquals(AppThemeMode.DARK, settingsRepository.getThemeMode().getOrThrow())
    }

    @Test
    fun swipingPastThePermissionsAsksForThemOnce() {
        swipeTo(OnboardingPage.RECEIVING)
        swipeTo(OnboardingPage.PERMISSIONS)
        swipeTo(OnboardingPage.RECEIVING)

        assertEquals(1, viewModel.uiState.value.permissionRequestId)
    }

    @Test
    fun afterAllowSwipingOnDoesNotAskAgain() {
        swipeTo(OnboardingPage.PERMISSIONS)
        viewModel.onUiEvent(OnboardingUiEvent.RequestPermissionsClicked)
        swipeTo(OnboardingPage.RECEIVING)

        assertEquals(1, viewModel.uiState.value.permissionRequestId)
    }

    @Test
    fun swipingPastReceivingSavesThePreselectedAlwaysConnectedAndOpensTheBatteryListOnce() {
        swipeTo(OnboardingPage.PHONE)
        swipeTo(OnboardingPage.RECEIVING)
        swipeTo(OnboardingPage.PHONE)

        assertEquals(listOf(ConnectionMode.ALWAYS_CONNECTED), store.saved)
        assertEquals(1, viewModel.uiState.value.batteryExemptionRequestId)
    }

    @Test
    fun aChosenModeIsNotSavedAgainWhenSwipingOn() {
        swipeTo(OnboardingPage.RECEIVING)
        viewModel.onUiEvent(OnboardingUiEvent.ConnectionModeChosen(ConnectionMode.WHILE_OPEN))
        swipeTo(OnboardingPage.PHONE)

        assertEquals(listOf(ConnectionMode.WHILE_OPEN), store.saved)
        assertEquals(0, viewModel.uiState.value.batteryExemptionRequestId)
    }

    @Test
    fun swipingBackDoesNothing() {
        viewModel.onUiEvent(OnboardingUiEvent.PageShown(OnboardingPage.RECEIVING))
        viewModel.onUiEvent(OnboardingUiEvent.PageShown(OnboardingPage.PERMISSIONS))

        assertEquals(0, viewModel.uiState.value.permissionRequestId)
        assertNull(store.saved.firstOrNull())
    }

    @Test
    fun thePermissionAnswerMovesOnOnlyFromThePermissionsPage() {
        swipeTo(OnboardingPage.PERMISSIONS)
        viewModel.onPermissionsResult(ANSWER)
        assertEquals(OnboardingPage.RECEIVING, viewModel.uiState.value.page)

        swipeTo(OnboardingPage.PHONE)
        viewModel.onPermissionsResult(ANSWER)
        assertEquals(OnboardingPage.PHONE, viewModel.uiState.value.page)
        assertTrue(viewModel.uiState.value.permissionsRequested)
    }

    /** Like a person swiping page by page. */
    private fun swipeTo(page: OnboardingPage) {
        val from = viewModel.uiState.value.page.ordinal
        val steps = if (page.ordinal >= from) from..page.ordinal else from downTo page.ordinal
        steps.forEach { index -> viewModel.onUiEvent(OnboardingUiEvent.PageShown(OnboardingPage.entries[index])) }
    }

    private class RecordingStore : ConnectionModeStore {
        val saved = mutableListOf<ConnectionMode>()

        override fun observe(): Flow<ConnectionMode> = emptyFlow()

        override suspend fun set(mode: ConnectionMode) {
            saved += mode
        }
    }

    private companion object {
        val ANSWER =
            PermissionRequestResult(
                contactsGranted = true,
                cameraGranted = true,
                audioGranted = true,
                notificationsGranted = true,
                phoneNumberGranted = false
            )
    }
}
