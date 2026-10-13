package com.cbgm.sparrow.feature.chats.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.MutableCreationExtras
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.annotation.KoinViewModelScopeApi
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.option.viewModelScopeFactory
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.factory.KoinViewModelFactory
import org.koin.viewmodel.scope.viewModelScope
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(KoinExperimentalAPI::class, KoinViewModelScopeApi::class, KoinInternalApi::class)
class ViewModelScopedControllersTest {
    private class Shared

    private class Consumer(
        val shared: Shared
    )

    private class TestViewModel(
        val shared: Shared,
        val consumer: Consumer
    ) : ViewModel()

    private val koin =
        koinApplication {
            options(viewModelScopeFactory())
            modules(
                module {
                    viewModelScope {
                        scopedOf(::Shared)
                        scopedOf(::Consumer)
                    }
                    viewModel { TestViewModel(get(), get()) }
                }
            )
        }.koin

    private fun create(): TestViewModel =
        KoinViewModelFactory(TestViewModel::class, koin.scopeRegistry.rootScope)
            .create(TestViewModel::class, MutableCreationExtras())

    @Test
    fun sharesOneControllerInstanceWithinViewModel() {
        val viewModel = create()

        assertSame(viewModel.shared, viewModel.consumer.shared)
    }

    @Test
    fun createsNewControllerInstancesPerViewModel() {
        assertNotSame(create().shared, create().shared)
    }
}
