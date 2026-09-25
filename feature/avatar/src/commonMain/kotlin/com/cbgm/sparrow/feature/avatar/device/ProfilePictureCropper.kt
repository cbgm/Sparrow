package com.cbgm.sparrow.feature.avatar.device

import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion

internal expect fun cropAndEncodeProfilePicture(
    sourceBytes: ByteArray,
    cropRegion: ProfilePictureCropRegion
): ByteArray
