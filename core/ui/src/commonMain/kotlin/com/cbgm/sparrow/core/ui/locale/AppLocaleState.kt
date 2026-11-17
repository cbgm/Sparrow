package com.cbgm.sparrow.core.ui.locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app language. A flow, not Compose state: the startup task sets it on a background thread while the first
 * composition may already run, and Compose state first touched there is invisible to that composition (it crashed with
 * "Reading a state that was created after the snapshot was taken").
 */
private val appLanguage = MutableStateFlow(AppLanguage.ENGLISH)

val currentAppLanguage: StateFlow<AppLanguage> = appLanguage.asStateFlow()

fun setAppLanguage(
    language: AppLanguage
) {
    appLanguage.value = language
}
