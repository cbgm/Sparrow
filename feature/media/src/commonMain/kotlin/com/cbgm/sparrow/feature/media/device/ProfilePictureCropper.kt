package com.cbgm.sparrow.feature.media.device

import com.cbgm.sparrow.feature.media.domain.model.ProfilePictureCropRegion

internal expect fun cropAndEncodeProfilePicture(
    sourceBytes: ByteArray,
    cropRegion: ProfilePictureCropRegion
): ByteArray?
