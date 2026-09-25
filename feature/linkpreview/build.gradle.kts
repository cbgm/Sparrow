plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
    alias(libs.plugins.sparrow.kmp.serialization)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.linkpreview"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.ui)
            implementation(projects.data.database)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
