plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.safety"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.ui)
            implementation(projects.feature.contacts)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
