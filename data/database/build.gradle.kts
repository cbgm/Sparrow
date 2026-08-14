plugins {
    alias(libs.plugins.sparrow.kmp.room)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.data.database"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.protocol)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.room.runtime)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
