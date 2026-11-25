package com.cbgm.sparrow.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Thin line icons for the top bars: the same shapes as the Material ones, drawn with a 1.5 stroke
 * instead of a 2 stroke fill.
 */
object SparrowIcons {
    val ArrowBack: ImageVector by lazy {
        lineIcon(name = "ArrowBack", autoMirror = true, "M20,12 H4", "M10,6 L4,12 L10,18")
    }

    val Close: ImageVector by lazy {
        lineIcon(name = "Close", autoMirror = false, "M6,6 L18,18", "M18,6 L6,18")
    }

    val Search: ImageVector by lazy {
        lineIcon(
            name = "Search",
            autoMirror = false,
            "M10.5,4 A6.5,6.5 0 1,1 10.49,4 Z",
            "M15.5,15.5 L20,20"
        )
    }

    val Mail: ImageVector by lazy {
        lineIcon(
            name = "Mail",
            autoMirror = false,
            "M4.5,5.5 H19.5 A1.5,1.5 0 0,1 21,7 V17 A1.5,1.5 0 0,1 19.5,18.5 H4.5 " +
                "A1.5,1.5 0 0,1 3,17 V7 A1.5,1.5 0 0,1 4.5,5.5 Z",
            "M3.5,7 L12,13 L20.5,7"
        )
    }

    val Check: ImageVector by lazy {
        lineIcon(name = "Check", autoMirror = false, "M5,12.5 L10,17.5 L19,7")
    }

    val Edit: ImageVector by lazy {
        lineIcon(
            name = "Edit",
            autoMirror = false,
            "M4,20 L4.6,16 L16,4.6 L19.4,8 L8,19.4 Z",
            "M13.8,6.8 L17.2,10.2"
        )
    }

    val Attach: ImageVector by lazy {
        lineIcon(
            name = "Attach",
            autoMirror = false,
            "M16,7 V16 A4,4 0 0,1 8,16 V6 A2.5,2.5 0 0,1 13,6 V15 A1,1 0 0,1 11,15 V7"
        )
    }

    val AttachOpen: ImageVector by lazy {
        lineIcon(
            name = "AttachOpen",
            autoMirror = false,
            "M7,8 H16 A4,4 0 0,1 16,16 H6 A2.5,2.5 0 0,1 6,11 H15 A1,1 0 0,1 15,13 H7"
        )
    }

    val Send: ImageVector by lazy {
        lineIcon(name = "Send", autoMirror = true, "M4,12 L20,4 L15,20 L12,13 Z", "M12,13 L20,4")
    }

    val Mic: ImageVector by lazy {
        lineIcon(
            name = "Mic",
            autoMirror = false,
            "M12,3 A3,3 0 0,1 15,6 V11 A3,3 0 0,1 9,11 V6 A3,3 0 0,1 12,3 Z",
            "M5.5,11 A6.5,6.5 0 0,0 18.5,11",
            "M12,17.5 V21"
        )
    }

    val Group: ImageVector by lazy {
        lineIcon(
            name = "Group",
            autoMirror = false,
            "M9,6 A3,3 0 1,1 8.99,6 Z",
            "M3.5,18 C4.3,15 6.5,13.5 9,13.5 C11.5,13.5 13.7,15 14.5,18",
            "M16,8 A2.3,2.3 0 1,1 15.99,8 Z",
            "M15.5,14 C17.8,14 19.6,15.2 20.5,18"
        )
    }

    val Receipt: ImageVector by lazy {
        lineIcon(
            name = "Receipt",
            autoMirror = false,
            "M6,3.5 H18 V20.5 L15.5,19 L13,20.5 L10.5,19 L8,20.5 L6,19 Z",
            "M9,8 H15 M9,11.5 H15 M9,15 H12.5"
        )
    }

    val Copy: ImageVector by lazy {
        lineIcon(
            name = "Copy",
            autoMirror = false,
            "M9,9 H20 V20 H9 Z",
            "M5,15 V4 H16"
        )
    }

    val QrCode: ImageVector by lazy {
        lineIcon(
            name = "QrCode",
            autoMirror = false,
            "M4,4 H10 V10 H4 Z",
            "M14,4 H20 V10 H14 Z",
            "M4,14 H10 V20 H4 Z",
            "M14,14 H16 V16 H14 Z",
            "M18,18 H20 V20 H18 Z",
            "M14,18 H16"
        )
    }

    val Chevron: ImageVector by lazy {
        lineIcon(name = "Chevron", autoMirror = true, "M10,7 L15,12 L10,17")
    }

    val Phone: ImageVector by lazy {
        lineIcon(
            name = "Phone",
            autoMirror = false,
            "M6.5,4 H9.5 L11,8 L9,9.5 A10,10 0 0,0 14.5,15 L16,13 L20,14.5 V17.5 A1.5,1.5 0 0,1 18.4,19 A15,15 0 0,1 5,5.6 A1.5,1.5 0 0,1 6.5,4 Z"
        )
    }

    val ContactCard: ImageVector by lazy {
        lineIcon(
            name = "ContactCard",
            autoMirror = false,
            "M4.5,5.5 H19.5 A1,1 0 0,1 20.5,6.5 V17.5 A1,1 0 0,1 19.5,18.5 H4.5 A1,1 0 0,1 3.5,17.5 V6.5 A1,1 0 0,1 4.5,5.5 Z",
            "M9,10.5 A2,2 0 1,1 8.99,10.5 Z",
            "M5.8,16 C6.3,14.5 7.5,13.8 9,13.8 C10.5,13.8 11.7,14.5 12.2,16",
            "M14.5,10 H18 M14.5,13 H17"
        )
    }

    val Shield: ImageVector by lazy {
        lineIcon(
            name = "Shield",
            autoMirror = false,
            "M12,3.5 L19,6 V11.5 C19,15.8 16,19.1 12,20.5 C8,19.1 5,15.8 5,11.5 V6 Z"
        )
    }

    val Folder: ImageVector by lazy {
        lineIcon(
            name = "Folder",
            autoMirror = false,
            "M3.5,7.5 A1.5,1.5 0 0,1 5,6 H9 L11,8 H19 A1.5,1.5 0 0,1 20.5,9.5 V17.5 A1.5,1.5 0 0,1 19,19 H5 A1.5,1.5 0 0,1 3.5,17.5 Z"
        )
    }

    val Share: ImageVector by lazy {
        lineIcon(
            name = "Share",
            autoMirror = false,
            "M17,3.5 A2.5,2.5 0 1,1 16.99,3.5 Z",
            "M7,9.5 A2.5,2.5 0 1,1 6.99,9.5 Z",
            "M17,15.5 A2.5,2.5 0 1,1 16.99,15.5 Z",
            "M9.2,10.8 L14.8,7.2",
            "M9.2,13.2 L14.8,16.8"
        )
    }

    val Leave: ImageVector by lazy {
        lineIcon(name = "Leave", autoMirror = true, "M14,5 H6 V19 H14", "M11,12 H20", "M17,9 L20,12 L17,15")
    }

    val Fingerprint: ImageVector by lazy {
        lineIcon(
            name = "Fingerprint",
            autoMirror = false,
            "M6.5,9 A6,6 0 0,1 17.5,9",
            "M8.5,19 C7.5,17 7,15 7,12.5 A5,5 0 0,1 17,12.5 C17,14 16.8,15.3 16.5,16.5",
            "M12,12.5 C12,15.5 11.2,18 10,20",
            "M14.5,13 C14.5,15.5 14,17.5 13.2,19.5"
        )
    }

    val Backup: ImageVector by lazy {
        lineIcon(
            name = "Backup",
            autoMirror = false,
            "M7,18.5 H6.5 A4,4 0 0,1 6.2,10.5 A6,6 0 0,1 17.6,9.5 A4.5,4.5 0 0,1 17.5,18.5 H17",
            "M12,19.5 V12",
            "M9,15 L12,12 L15,15"
        )
    }

    val ShieldCheck: ImageVector by lazy {
        lineIcon(
            name = "ShieldCheck",
            autoMirror = false,
            "M12,3.5 L19,6 V11.5 C19,15.8 16,19.1 12,20.5 C8,19.1 5,15.8 5,11.5 V6 Z",
            "M9,12 L11,14 L15,10"
        )
    }

    val ShieldAlert: ImageVector by lazy {
        lineIcon(
            name = "ShieldAlert",
            autoMirror = false,
            "M12,3.5 L19,6 V11.5 C19,15.8 16,19.1 12,20.5 C8,19.1 5,15.8 5,11.5 V6 Z",
            "M12,8.5 V12.5",
            "M12,15.5 V15.6"
        )
    }

    val Block: ImageVector by lazy {
        lineIcon(
            name = "Block",
            autoMirror = false,
            "M12,4 A8,8 0 1,1 11.99,4 Z",
            "M6.4,6.4 L17.6,17.6"
        )
    }

    val Add: ImageVector by lazy {
        lineIcon(name = "Add", autoMirror = false, "M12,5 V19", "M5,12 H19")
    }

    val Delete: ImageVector by lazy {
        lineIcon(
            name = "Delete",
            autoMirror = false,
            "M4.5,6.5 H19.5",
            "M9.5,6.5 V4.5 H14.5 V6.5",
            "M6.5,6.5 L7.5,19.5 H16.5 L17.5,6.5",
            "M10,10 V16",
            "M14,10 V16"
        )
    }

    val More: ImageVector by lazy {
        lineIcon(
            name = "More",
            autoMirror = false,
            "M12,5.2 A0.8,0.8 0 1,1 11.99,5.2 Z",
            "M12,11.2 A0.8,0.8 0 1,1 11.99,11.2 Z",
            "M12,17.2 A0.8,0.8 0 1,1 11.99,17.2 Z"
        )
    }

    val Refresh: ImageVector by lazy {
        lineIcon(
            name = "Refresh",
            autoMirror = false,
            "M19,12 A7,7 0 1,1 16.95,7.05",
            "M17,3.5 V7.5 H13"
        )
    }

    val SelectAll: ImageVector by lazy {
        lineIcon(
            name = "SelectAll",
            autoMirror = false,
            "M5.5,4.5 H18.5 A1,1 0 0,1 19.5,5.5 V18.5 A1,1 0 0,1 18.5,19.5 H5.5 A1,1 0 0,1 4.5,18.5 V5.5 A1,1 0 0,1 5.5,4.5 Z",
            "M8.5,12 L11,14.5 L15.5,9.5"
        )
    }

    val Deselect: ImageVector by lazy {
        lineIcon(
            name = "Deselect",
            autoMirror = false,
            "M5.5,4.5 H18.5 A1,1 0 0,1 19.5,5.5 V18.5 A1,1 0 0,1 18.5,19.5 H5.5 A1,1 0 0,1 4.5,18.5 V5.5 A1,1 0 0,1 5.5,4.5 Z"
        )
    }

    private fun lineIcon(
        name: String,
        autoMirror: Boolean,
        vararg paths: String
    ): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = IconSize.dp,
            defaultHeight = IconSize.dp,
            viewportWidth = IconSize,
            viewportHeight = IconSize,
            autoMirror = autoMirror
        ).apply {
            paths.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = StrokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    private const val IconSize = 24f
    private const val StrokeWidth = 1.5f
}
