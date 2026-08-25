package com.cbgm.sparrow.navigation.presentation.main.model

import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.ic_chat
import com.cbgm.sparrow.resources.ic_chat_outlined
import com.cbgm.sparrow.resources.ic_identity
import com.cbgm.sparrow.resources.ic_identity_outlined
import com.cbgm.sparrow.resources.ic_settings
import com.cbgm.sparrow.resources.ic_settings_outlined
import com.cbgm.sparrow.resources.main_title_and_nav_chats
import com.cbgm.sparrow.resources.main_title_and_nav_identity
import com.cbgm.sparrow.resources.main_title_and_nav_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class MainTab(
    val label: StringResource,
    val res: DrawableResource,
    val resOutlined: DrawableResource
) {
    Chats(
        label = Res.string.main_title_and_nav_chats,
        res = Res.drawable.ic_chat,
        resOutlined = Res.drawable.ic_chat_outlined
    ),
    Me(
        label = Res.string.main_title_and_nav_identity,
        res = Res.drawable.ic_identity,
        resOutlined = Res.drawable.ic_identity_outlined
    ),
    Settings(
        label = Res.string.main_title_and_nav_settings,
        res = Res.drawable.ic_settings,
        resOutlined = Res.drawable.ic_settings_outlined
    )
}
