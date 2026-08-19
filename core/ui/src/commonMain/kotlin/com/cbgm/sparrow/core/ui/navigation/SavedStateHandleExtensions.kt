package com.cbgm.sparrow.core.ui.navigation

import androidx.lifecycle.SavedStateHandle

fun <T : Any> SavedStateHandle.requireRouteArgument(key: String): T =
    requireNotNull(get<T>(key)) {
        "Missing navigation argument '$key'"
    }
