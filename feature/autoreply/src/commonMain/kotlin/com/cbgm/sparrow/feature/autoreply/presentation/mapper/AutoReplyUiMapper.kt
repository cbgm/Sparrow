package com.cbgm.sparrow.feature.autoreply.presentation.mapper

import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyUiItem

internal fun AutoReply.toUiItem(): AutoReplyUiItem =
    AutoReplyUiItem(
        id = id,
        name = name,
        text = text,
        isActive = isActive
    )
