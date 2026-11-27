package com.cbgm.sparrow.core.ui.device.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.core_background_help_asus
import com.cbgm.sparrow.resources.core_background_help_huawei
import com.cbgm.sparrow.resources.core_background_help_oppo
import com.cbgm.sparrow.resources.core_background_help_samsung
import com.cbgm.sparrow.resources.core_background_help_vivo
import com.cbgm.sparrow.resources.core_background_help_xiaomi
import org.jetbrains.compose.resources.StringResource

/**
 * On phones whose maker stops apps in the background ([PhoneMaker]): what to set so the connection kept while Sparrow is
 * closed survives, and a button that opens that setting (or Sparrow's app settings when the maker's screen cannot be
 * opened). Shows nothing on other phones and on iOS.
 */
@Composable
expect fun BackgroundRestrictionHelp(modifier: Modifier = Modifier)

/** The instruction shown for this maker. */
internal fun PhoneMaker.instruction(): StringResource =
    when (this) {
        PhoneMaker.XIAOMI -> Res.string.core_background_help_xiaomi
        PhoneMaker.HUAWEI, PhoneMaker.HONOR -> Res.string.core_background_help_huawei
        PhoneMaker.OPPO -> Res.string.core_background_help_oppo
        PhoneMaker.VIVO -> Res.string.core_background_help_vivo
        PhoneMaker.SAMSUNG -> Res.string.core_background_help_samsung
        PhoneMaker.ASUS -> Res.string.core_background_help_asus
    }
