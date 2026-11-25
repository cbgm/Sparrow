package com.cbgm.sparrow.navigation.presentation.main.model

import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.main_title_and_nav_chats
import com.cbgm.sparrow.resources.main_title_and_nav_identity
import com.cbgm.sparrow.resources.main_title_and_nav_settings
import org.jetbrains.compose.resources.StringResource

enum class MainTab(
    val label: StringResource
) {
    Chats(
        label = Res.string.main_title_and_nav_chats
    ),
    Me(
        label = Res.string.main_title_and_nav_identity
    ),
    Settings(
        label = Res.string.main_title_and_nav_settings
    )
}
