plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
