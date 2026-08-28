plugins {
    alias(libs.plugins.sparrow.kmp.library)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.core.embedding"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.data.datastore)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
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
