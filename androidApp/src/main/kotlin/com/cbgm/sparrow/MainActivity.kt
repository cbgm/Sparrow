package com.cbgm.sparrow

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cbgm.sparrow.device.consumeAndroidAppIntent
import com.cbgm.sparrow.presentation.App

class MainActivity : ComponentActivity() {
    // Read by Android's splash pre-draw callback on the UI thread.
    private var startupContentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashStartedAt = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !startupContentReady &&
                SystemClock.uptimeMillis() - splashStartedAt < 5_000L
        }
        super.onCreate(savedInstanceState)

        consumeAndroidAppIntent(intent)
        setContent {
            App(onStartupContentReady = { startupContentReady = true })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAndroidAppIntent(intent)
    }
}
