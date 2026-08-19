package com.cbgm.sparrow.startup

sealed interface AppInitializationResult {
    data object IdentityRequired : AppInitializationResult

    data object ReadyOnline : AppInitializationResult

    data object ReadyOffline : AppInitializationResult
}
