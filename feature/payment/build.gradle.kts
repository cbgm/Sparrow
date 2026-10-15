plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.sparrow.kmp.serialization)
}
kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.payment"
        androidResources { enable = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.base)
            implementation(projects.feature.media)
            implementation(projects.core.ui)
            implementation(projects.core.util)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.bundles.serialization)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
        }
        commonTest.dependencies { implementation(libs.bundles.kmp.testing) }
        androidHostTest.dependencies {
            implementation(libs.bundles.android.host.testing)
            implementation(libs.robolectric)
        }
        androidDeviceTest.dependencies {
            implementation(libs.zxing.core)
            implementation(libs.bundles.android.device.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
