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
    private var nativeSplashReleased = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashStartedAt = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !nativeSplashReleased &&
                SystemClock.uptimeMillis() - splashStartedAt < 5_000L
        }
        super.onCreate(savedInstanceState)

        consumeAndroidAppIntent(intent)
        setContent {
            App(onReleaseNativeSplash = { nativeSplashReleased = true })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAndroidAppIntent(intent)
    }
}
