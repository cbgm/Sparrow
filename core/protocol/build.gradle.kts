plugins {
    alias(libs.plugins.sparrow.kmp.serialization)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.protocol"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
