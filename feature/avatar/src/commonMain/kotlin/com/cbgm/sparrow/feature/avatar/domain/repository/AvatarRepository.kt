package com.cbgm.sparrow.feature.avatar.domain.repository

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarImage
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import kotlinx.coroutines.flow.Flow

interface AvatarRepository {
    fun observe(target: AvatarTarget): Flow<Result<AvatarImage>>
}
