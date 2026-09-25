plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.membership"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.crypto)
            implementation(projects.core.protocol)
            implementation(projects.data.database)
            implementation(projects.data.datastore)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
            implementation(libs.kotlinx.coroutines.test)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.bundles.kmp.testing)
        }
    }
}
