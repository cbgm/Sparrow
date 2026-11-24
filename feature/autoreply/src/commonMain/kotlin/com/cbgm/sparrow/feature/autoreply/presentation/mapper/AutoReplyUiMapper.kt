package com.cbgm.sparrow.feature.autoreply.presentation.mapper

import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyItemUi

internal fun AutoReply.toAutoReplyItemUi(): AutoReplyItemUi =
    AutoReplyItemUi(
        id = id,
        name = name,
        text = text,
        isActive = isActive
    )
