package com.cbgm.sparrow.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UIScreenCapturedDidChangeNotification
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BlockScreenshotEffect(enabled: Boolean) {
    val viewController = LocalUIViewController.current

    DisposableEffect(viewController, enabled) {
        if (!enabled) {
            onDispose {}
        } else {
            val hostView =
                viewController.view.window
                    ?: viewController.view

            val protectionView =
                UIView(frame = hostView.bounds).apply {
                    backgroundColor = UIColor.blackColor
                    userInteractionEnabled = true
                    hidden = true
                    autoresizingMask =
                        UIViewAutoresizingFlexibleWidth or
                            UIViewAutoresizingFlexibleHeight
                }

            hostView.addSubview(protectionView)

            var appIsActive = true

            fun updateProtection() {
                val shouldCoverContent =
                    UIScreen.mainScreen.captured ||
                        !appIsActive

                protectionView.hidden = !shouldCoverContent

                if (shouldCoverContent) {
                    protectionView.frame = hostView.bounds
                    hostView.bringSubviewToFront(protectionView)
                }
            }

            val notificationCenter =
                NSNotificationCenter.defaultCenter

            val captureObserver =
                notificationCenter.addObserverForName(
                    name = UIScreenCapturedDidChangeNotification,
                    `object` = UIScreen.mainScreen,
                    queue = NSOperationQueue.mainQueue
                ) {
                    updateProtection()
                }

            val resignActiveObserver =
                notificationCenter.addObserverForName(
                    name = UIApplicationWillResignActiveNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue
                ) {
                    appIsActive = false
                    updateProtection()
                }

            val becomeActiveObserver =
                notificationCenter.addObserverForName(
                    name = UIApplicationDidBecomeActiveNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue
                ) {
                    appIsActive = true
                    updateProtection()
                }

            updateProtection()

            onDispose {
                notificationCenter.removeObserver(captureObserver)
                notificationCenter.removeObserver(resignActiveObserver)
                notificationCenter.removeObserver(becomeActiveObserver)

                protectionView.removeFromSuperview()
            }
        }
    }
}
