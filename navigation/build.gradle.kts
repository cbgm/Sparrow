plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.sparrow.kmp.serialization)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.navigation"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.ui)
            implementation(projects.feature.chats)
            implementation(projects.feature.contactimport)
            implementation(projects.feature.contacts)
            implementation(projects.feature.identity)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.settings)
            implementation(projects.feature.search)
            implementation(projects.notification)
            implementation(projects.startup)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.bundles.serialization)

            implementation(libs.jetbrains.navigation.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
