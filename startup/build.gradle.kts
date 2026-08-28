plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.startup"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.core.embedding)
            implementation(projects.feature.identity)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.search)
            implementation(projects.feature.safety)
            implementation(projects.feature.transport)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
