package com.cbgm.sparrow.core.ui.locale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

var currentAppLanguage by mutableStateOf(AppLanguage.ENGLISH)
    private set

fun setAppLanguage(
    language: AppLanguage
) {
    currentAppLanguage = language
}
