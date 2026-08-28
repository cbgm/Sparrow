package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFileAccessLauncher(
    onReturned: (String?) -> Unit,
    onError: (String) -> Unit
): FileAccessLauncher {
    val viewController = LocalUIViewController.current
    val currentOnReturned = rememberUpdatedState(onReturned)
    val currentOnError = rememberUpdatedState(onError)
    val delegate =
        remember {
            FileAccessDelegate(
                onReturned = { reference -> currentOnReturned.value(reference) },
                onError = { message -> currentOnError.value(message) }
            )
        }

    return remember(viewController, delegate) {
        object : FileAccessLauncher {
            override fun launch() {
                runCatching {
                    val picker =
                        UIDocumentPickerViewController(
                            forOpeningContentTypes = listOf(UTTypeFolder),
                            asCopy = false
                        )
                    picker.delegate = delegate
                    viewController.presentViewController(picker, animated = true, completion = null)
                }.onFailure { error ->
                    currentOnError.value(error.message ?: "File access could not be opened")
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class FileAccessDelegate(
    private val onReturned: (String?) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        val reference =
            (didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL)
                ?.absoluteString
        if (reference == null) {
            onError("Selected directory could not be opened")
        } else {
            onReturned(reference)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
        onReturned(null)
    }
}
