package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
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
        localFilePath: String,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        runCatching {
            require(localFilePath.isNotBlank()) { "File path must not be blank" }
            require(NSFileManager.defaultManager.fileExistsAtPath(localFilePath)) {
                "File does not exist: $localFilePath"
            }

            val url = NSURL.fileURLWithPath(localFilePath)
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
