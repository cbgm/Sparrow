package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cbgm.sparrow.core.result.safeSuspendCall
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun rememberFileOpener(): FileOpener =
    remember {
        IosFileOpener()
    }

private class IosFileOpener : FileOpener {
    private var documentInteractionController: UIDocumentInteractionController? = null

    override suspend fun open(
        localFilePath: String,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        safeSuspendCall {
            val url = NSURL.fileURLWithPath(localFilePath)

            require(
                NSFileManager.defaultManager.fileExistsAtPath(localFilePath)
            ) {
                "File does not exist: $localFilePath"
            }

            openFile(url)
        }

    override suspend fun open(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        safeSuspendCall {
            require(bytes.isNotEmpty()) {
                "File bytes must not be empty"
            }

            val path =
                buildString {
                    append(NSTemporaryDirectory())
                    append("sparrow-")
                    append(fileName)
                }

            bytes
                .toNSData()
                .writeToFile(
                    path = path,
                    atomically = true
                )

            openFile(
                NSURL.fileURLWithPath(path)
            )
        }

    private fun openFile(url: NSURL) {
        val viewController =
            UIApplication.sharedApplication
                .keyWindow
                ?.rootViewController
                ?.topViewController()
                ?: error("No UIViewController available")

        val controller =
            UIDocumentInteractionController.interactionControllerWithURL(url)

        controller.delegate =
            DocumentInteractionDelegate(viewController)

        documentInteractionController = controller

        controller.presentPreviewAnimated(true)
    }
}

private class DocumentInteractionDelegate(
    private val viewController: UIViewController
) : NSObject(),
    UIDocumentInteractionControllerDelegateProtocol {
    override fun documentInteractionControllerViewControllerForPreview(
        controller: UIDocumentInteractionController
    ): UIViewController =
        viewController
}

private fun UIViewController.topViewController(): UIViewController {
    val presented = presentedViewController
    return presented?.topViewController() ?: this
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong()
        )
    }
