package com.cbgm.sparrow.feature.avatar.device

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion
import java.io.ByteArrayOutputStream

internal actual fun cropAndEncodeProfilePicture(
    sourceBytes: ByteArray,
    cropRegion: ProfilePictureCropRegion
): ByteArray {
    val bitmap =
        requireNotNull(decodeProfilePictureBitmap(sourceBytes)) {
            "Profile picture could not be decoded"
        }
    val sourceRect =
        cropRegion.toProfilePictureSourceRect(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )

    val cropped =
        Bitmap.createBitmap(
            bitmap,
            sourceRect.left,
            sourceRect.top,
            sourceRect.size,
            sourceRect.size
        )
    val scaled =
        if (
            sourceRect.size == PROFILE_PICTURE_SIZE &&
            cropped.width == PROFILE_PICTURE_SIZE &&
            cropped.height == PROFILE_PICTURE_SIZE
        ) {
            cropped
        } else {
            cropped.scale(PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE)
        }

    return try {
        ByteArrayOutputStream().use { output ->
            check(
                scaled.compress(
                    Bitmap.CompressFormat.JPEG,
                    PROFILE_PICTURE_JPEG_QUALITY,
                    output
                )
            ) {
                "Profile picture could not be encoded"
            }
            output.toByteArray()
        }
    } finally {
        if (scaled !== cropped) {
            scaled.recycle()
        }
        cropped.recycle()
        bitmap.recycle()
    }
}

private const val PROFILE_PICTURE_SIZE = 512
private const val PROFILE_PICTURE_JPEG_QUALITY = 88
