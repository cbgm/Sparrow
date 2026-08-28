val isMacOs =
    System
        .getProperty("os.name")
        .startsWith(
            prefix = "Mac",
            ignoreCase = true
        )

plugins {
    alias(libs.plugins.sparrow.kmp.serialization)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.transport"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.data.datastore)
            implementation(projects.core.crypto)
            implementation(projects.core.protocol)

            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)
            implementation(libs.bundles.ktor.client)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        if (isMacOs) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
