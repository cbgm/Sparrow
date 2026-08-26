package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFileOpener(): FileOpener {
    val viewController = LocalUIViewController.current
    return remember(viewController) { IosFileOpener(viewController) }
}

@OptIn(ExperimentalForeignApi::class)
private class IosFileOpener(
    private val viewController: UIViewController
) : FileOpener {
    private val delegate = FileInteractionDelegate(viewController)
    private var interactionController: UIDocumentInteractionController? = null

    override suspend fun open(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<Unit> =
        runCatching {
            require(fileName.isNotBlank()) { "File name must not be blank" }
            require(bytes.isNotEmpty()) { "File is empty" }

            val url = createTemporaryFile(fileName, bytes)
                ?: error("File could not be prepared")
            val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
            controller.delegate = delegate
            interactionController = controller
            check(controller.presentPreviewAnimated(true)) { "No app can preview this file" }
        }
}

@OptIn(ExperimentalForeignApi::class)
private class FileInteractionDelegate(
    private val viewController: UIViewController
) : NSObject(),
    UIDocumentInteractionControllerDelegateProtocol {
    override fun documentInteractionControllerViewControllerForPreview(
        controller: UIDocumentInteractionController
    ): UIViewController = viewController
}

@OptIn(ExperimentalForeignApi::class)
private fun createTemporaryFile(
    fileName: String,
    bytes: ByteArray
): NSURL? {
    val url =
        NSFileManager.defaultManager.temporaryDirectory
            .URLByAppendingPathComponent(fileName.safeFileName())
            ?: return null
    val data =
        bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong()
            )
        }
    return url.takeIf { data.writeToURL(it, atomically = true) }
}

private fun String.safeFileName(): String {
    val sanitized =
        replace(Regex("[/:]"), "_")
            .trim()
            .take(MAX_FILE_NAME_LENGTH)
    return sanitized.ifBlank { "sparrow-file" }
}

private const val MAX_FILE_NAME_LENGTH = 180
