plugins {
    alias(libs.plugins.sparrow.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.search"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.datastore)
            implementation(projects.core.ui)
            implementation(projects.data.database)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime)
            implementation(libs.koin.android)
            implementation(libs.mediapipe.tasks.text)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
