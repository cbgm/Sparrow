plugins {
    alias(libs.plugins.sparrow.kmp.library)
    alias(libs.plugins.sparrow.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.sparrow.feature.embedding"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.util)
            implementation(projects.data.datastore)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.work.runtime)
            implementation(libs.koin.android)
            implementation(libs.mediapipe.tasks.text)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.host.testing)
        }
    }
}
